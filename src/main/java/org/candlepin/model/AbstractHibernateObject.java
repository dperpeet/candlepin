/**
 * Copyright (c) 2009 - 2012 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.candlepin.jackson.DynamicFilterable;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * Abstract class for hibernate entities
 */
@MappedSuperclass
@XmlType(name = "CandlepinObject")
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractHibernateObject implements Persisted,
        Serializable, DynamicFilterable {
    public static final String DEFAULT_SORT_FIELD = "created";

    private Date created;
    private Date updated;

    // Attributes for dynamic filtering
    @Transient
    private Set<String> filterList;
    @Transient
    private boolean blacklist = false;

    @PrePersist
    protected void onCreate() {
        Date now = new Date();

        setCreated(now);
        setUpdated(now);
    }

    @PreUpdate
    protected void onUpdate() {
        setUpdated(new Date());
    }

    @XmlElement
    @Column(nullable = false, unique = true)
    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    @XmlElement
    @Column(nullable = false, unique = true)
    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    @XmlTransient
    public void setBlacklist(boolean blacklist) {
        this.blacklist = blacklist;
        this.filterList = new HashSet<String>();
    }

    @XmlTransient
    public boolean isAttributeFiltered(String attribute) {
        return filterList != null && // Break off early if filterList is null
            (blacklist && filterList.contains(attribute) ||
            !blacklist && !filterList.contains(attribute));
    }

    @XmlTransient
    public void filterAttribute(String attribute) {
        if (filterList == null) {
            //first call decides whether we are using a blacklist or whitelist
            blacklist = true;
            filterList = new HashSet<String>();
        }
        // Works differently if we are using blacklist/whitelist
        if (blacklist) {
            filterList.add(attribute);
        }
        else {
            filterList.remove(attribute);
        }
    }

    @XmlTransient
    public void allowAttribute(String attribute) {
        if (filterList == null) {
            blacklist = false;
            filterList = new HashSet<String>();
        }
        if (!blacklist) {
            filterList.add(attribute);
        }
        else {
            filterList.remove(attribute);
        }
    }
}
