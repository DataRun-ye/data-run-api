package org.nmcpye.datarun.submission.feed.publisher;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.config.activation.ActivationSwitches;
import org.nmcpye.datarun.submission.feed.FeedActivation;
import org.nmcpye.datarun.submission.normalized.jdbc.FormDefinitionJdbcReader;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** While the switch is off, the dictionary's publisher reads nothing and writes nothing, at start or later. */
class DictionaryPublisherTest {

    @Test
    void withTheSwitchOffNothingIsReadOrWritten() {
        ActivationSwitches switches = mock(ActivationSwitches.class);
        when(switches.isOn(FeedActivation.SUBMISSION_FEED)).thenReturn(false);
        FormDefinitionJdbcReader reader = mock(FormDefinitionJdbcReader.class);
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        DictionaryPublisher publisher = new DictionaryPublisher(switches, reader, jdbc, transactions);

        publisher.publishAtStart();
        publisher.publishOnSchedule();

        assertThat(publisher.publishIfChanged()).isFalse();
        verifyNoInteractions(reader, jdbc, transactions);
    }
}
