package org.nmcpye.datarun.jpa.option.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.nmcpye.datarun.jpa.option.Option;

import java.util.List;

public class OptionRepositoryCustomImpl
    implements OptionRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Option> getOptions(String optionSetId, String key, Integer max) {
        String hql = "select option from OptionSet as optionset " +
            "join optionset.options as option where optionset.id = :optionSetId ";

        if (key != null) {
            hql += "and lower(option.name) like lower(CONCAT('%', :key, '%')) ";
        }

        hql += "order by index(option)";

        Query query = entityManager.createQuery(hql, Option.class)
            .setParameter("optionSetId", optionSetId);

        if (key != null) {
            query.setParameter("key", key);
        }

        if (max != null) {
            query.setMaxResults(max);
        }

        return query.getResultList();
    }
}
