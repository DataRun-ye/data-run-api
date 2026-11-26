package org.nmcpye.datarun.submission.feed.publisher;

import org.nmcpye.datarun.common.exceptions.FailureShape;
import org.nmcpye.datarun.config.activation.ActivationSwitches;
import org.nmcpye.datarun.submission.feed.FeedActivation;
import org.nmcpye.datarun.submission.normalized.jdbc.FormDefinitionJdbcReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

/**
 * Publishes the feed's dictionary ({@code feed.form_field}, {@code feed.form_section}): rewrites it in place, in one
 * transaction, from every stored form definition, at start and whenever a form version is published or changed
 * (contract 6). Nothing is written while the switch {@code submission-feed} is off.
 *
 * <p><b>When.</b> Every {@code datarun.feed.dictionary-check-delay} (one minute) it compares the definitions' change
 * tokens ({@link FormDefinitionJdbcReader#allChangeTokens}, one cheap statement) with those of the definitions it last
 * wrote, and rewrites when they differ; its first check in a process always rewrites. The tokens move with every change
 * the dictionary is built from, by whatever path it comes:</p>
 * <ul>
 *   <li>a version published: {@code POST /api/v1/dataFormTemplates} (and {@code /bulk}, {@code /return}), through
 *       {@code DataTemplateInstanceService.publishVersion};</li>
 *   <li>a version rewritten or deleted: {@code PUT} and {@code DELETE /api/v1/formTemplateVersions/{id}}, the generic
 *       routes; its {@code POST} routes write nothing;</li>
 *   <li>a data element or option set that a version names created, changed or deleted, which changes the ids its
 *       fields resolve to: the generic {@code dataElements} and {@code optionSets} routes;</li>
 *   <li>anything written by SQL or a migration.</li>
 * </ul>
 * <p>A template's own row ({@code data_template}) gives the dictionary only its id, which never changes, so its
 * routes, its soft delete included, change nothing here; a deleted form's versions stay in the dictionary, as its
 * submissions stay in the feed.</p>
 *
 * <p><b>How.</b> One transaction: the dictionary's advisory lock, tried, so that two processes never rewrite at once
 * (a process that finds it held tries again at its next check); every definition read in one statement; every record
 * deleted and written again. A reader whose snapshot predates the commit reads the whole old dictionary, and one after
 * it the whole new one. A failure rolls everything back, is logged, and is tried again at the next check; it never
 * fails a start.</p>
 */
@Component
public class DictionaryPublisher {

    /** The dictionary's advisory lock: "feed" in ASCII, then 1. */
    static final long LOCK_KEY = 0x6665_6564_0000_0001L;

    private static final Logger log = LoggerFactory.getLogger(DictionaryPublisher.class);

    private static final String INSERT_SECTION = """
        INSERT INTO feed.form_section (form_id, form_version_id, form_version_number, section_id, parent_section_id,
                                       name, label, repeatable, sort_order, published_at)
        VALUES (:formId, :formVersionId, :formVersionNumber, :sectionId, :parentSectionId, :name,
                CAST(:label AS jsonb), :repeatable, :sortOrder, now())""";

    private static final String INSERT_FIELD = """
        INSERT INTO feed.form_field (form_id, form_version_id, form_version_number, field_id, section_id,
                                     repeat_section_id, name, label, value_type, option_set_id, mandatory, sort_order,
                                     published_at)
        VALUES (:formId, :formVersionId, :formVersionNumber, :fieldId, :sectionId, :repeatSectionId, :name,
                CAST(:label AS jsonb), :valueType, :optionSetId, :mandatory, :sortOrder, now())""";

    private final ActivationSwitches switches;
    private final FormDefinitionJdbcReader reader;
    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate writing;
    private final TransactionTemplate reading;

    /** The change tokens of the definitions this process last wrote, or {@code null} before its first rewrite. */
    private Map<String, String> published;

    public DictionaryPublisher(ActivationSwitches switches, FormDefinitionJdbcReader reader,
                               NamedParameterJdbcTemplate jdbc, PlatformTransactionManager transactionManager) {
        this.switches = switches;
        this.reader = reader;
        this.jdbc = jdbc;
        this.writing = new TransactionTemplate(transactionManager);
        this.reading = new TransactionTemplate(transactionManager);
        this.reading.setReadOnly(true);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void publishAtStart() {
        publishIfChanged();
    }

    @Scheduled(initialDelayString = "${datarun.feed.dictionary-check-delay:PT1M}",
        fixedDelayString = "${datarun.feed.dictionary-check-delay:PT1M}")
    public void publishOnSchedule() {
        publishIfChanged();
    }

    /**
     * Rewrites the dictionary when the switch is on and the stored definitions changed since this process last wrote
     * it, or it never did. Never throws.
     *
     * @return whether it rewrote the dictionary
     */
    public synchronized boolean publishIfChanged() {
        if (!switches.isOn(FeedActivation.SUBMISSION_FEED)) {
            return false;
        }
        try {
            if (published != null && published.equals(reading.execute(status -> reader.allChangeTokens()))) {
                return false;
            }
            Map<String, String> written = writing.execute(status -> rewrite());
            if (written == null) {
                log.debug("The feed's dictionary is being rewritten by another session; checked again later");
                return false;
            }
            published = written;
            return true;
        } catch (RuntimeException e) {
            log.warn("The feed's dictionary was not published, and is tried again at the next check: {}",
                FailureShape.of(e));
            return false;
        }
    }

    /** One rewrite, in the caller's transaction; the tokens of the definitions written, or {@code null} if locked. */
    private Map<String, String> rewrite() {
        jdbc.update("SET LOCAL lock_timeout = '5s'", Map.of());
        jdbc.update("SET LOCAL statement_timeout = '60s'", Map.of());
        Boolean locked = jdbc.queryForObject("SELECT pg_try_advisory_xact_lock(:key)", Map.of("key", LOCK_KEY),
            Boolean.class);
        if (!Boolean.TRUE.equals(locked)) {
            return null;
        }
        Map<String, FormDefinitionJdbcReader.Snapshot> forms = reader.readAll();
        FormDictionary dictionary = FormDictionary.of(forms.values().stream()
            .map(FormDefinitionJdbcReader.Snapshot::definition).toList());

        jdbc.update("DELETE FROM feed.form_field", Map.of());
        jdbc.update("DELETE FROM feed.form_section", Map.of());
        jdbc.batchUpdate(INSERT_SECTION, dictionary.sections().stream().map(DictionaryPublisher::parameters)
            .toArray(SqlParameterSource[]::new));
        jdbc.batchUpdate(INSERT_FIELD, dictionary.fields().stream().map(DictionaryPublisher::parameters)
            .toArray(SqlParameterSource[]::new));

        if (dictionary.pathsTakenByAnother().count() > 0) {
            log.warn("The feed's dictionary left out {} field placements at a data path their form version names "
                + "twice, which resolves to another data element (AMBIGUOUS_PATH), in versions {}",
                dictionary.pathsTakenByAnother().count(), dictionary.pathsTakenByAnother().versions());
        }
        if (dictionary.givenTwice().count() > 0) {
            log.warn("The feed's dictionary left out {} section definitions or field placements that a form version "
                + "gives twice in one section (the first in walk order is kept), in versions {}",
                dictionary.givenTwice().count(), dictionary.givenTwice().versions());
        }
        log.info("Published the feed's dictionary: {} field placements and {} sections of {} form versions",
            dictionary.fields().size(), dictionary.sections().size(), dictionary.versionCount());
        Map<String, String> tokens = new HashMap<>();
        forms.forEach((formId, snapshot) -> tokens.put(formId, snapshot.changeToken()));
        return Map.copyOf(tokens);
    }

    private static SqlParameterSource parameters(FormDictionary.SectionRecord section) {
        return new MapSqlParameterSource()
            .addValue("formId", section.formId(), Types.VARCHAR)
            .addValue("formVersionId", section.formVersionId(), Types.VARCHAR)
            .addValue("formVersionNumber", section.formVersionNumber(), Types.INTEGER)
            .addValue("sectionId", section.sectionId(), Types.VARCHAR)
            .addValue("parentSectionId", section.parentSectionId(), Types.VARCHAR)
            .addValue("name", section.name(), Types.VARCHAR)
            .addValue("label", section.label(), Types.VARCHAR)
            .addValue("repeatable", section.repeatable(), Types.BOOLEAN)
            .addValue("sortOrder", section.sortOrder(), Types.INTEGER);
    }

    private static SqlParameterSource parameters(FormDictionary.FieldRecord field) {
        return new MapSqlParameterSource()
            .addValue("formId", field.formId(), Types.VARCHAR)
            .addValue("formVersionId", field.formVersionId(), Types.VARCHAR)
            .addValue("formVersionNumber", field.formVersionNumber(), Types.INTEGER)
            .addValue("fieldId", field.fieldId(), Types.VARCHAR)
            .addValue("sectionId", field.sectionId(), Types.VARCHAR)
            .addValue("repeatSectionId", field.repeatSectionId(), Types.VARCHAR)
            .addValue("name", field.name(), Types.VARCHAR)
            .addValue("label", field.label(), Types.VARCHAR)
            .addValue("valueType", field.valueType(), Types.VARCHAR)
            .addValue("optionSetId", field.optionSetId(), Types.VARCHAR)
            .addValue("mandatory", field.mandatory(), Types.BOOLEAN)
            .addValue("sortOrder", field.sortOrder(), Types.INTEGER);
    }
}
