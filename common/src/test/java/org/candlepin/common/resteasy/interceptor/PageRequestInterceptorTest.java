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
package org.candlepin.common.resteasy.interceptor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.candlepin.common.exceptions.BadRequestException;
import org.candlepin.common.guice.CommonI18nProvider;
import org.candlepin.common.paging.PageRequest;

import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.junit.Before;
import org.junit.Test;
import org.xnap.commons.i18n.I18n;

import java.util.Locale;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

/**
 * PageRequestInterceptorTest
 */
public class PageRequestInterceptorTest {

    @Inject
    private HttpServletRequest mockReq = mock(HttpServletRequest.class);

    private javax.inject.Provider<I18n> i18nProvider;
    private PageRequestInterceptor interceptor;
    private ResourceMethodInvoker rminvoker;

    @Before
    public void setUp() throws Exception {
        when(mockReq.getLocale()).thenReturn(Locale.US);
        this.i18nProvider = new CommonI18nProvider(this.mockReq);
        interceptor = new PageRequestInterceptor(this.i18nProvider);
        rminvoker = mock(ResourceMethodInvoker.class);
    }

    @Test
    public void testNoAnything() throws Exception {
        MockHttpRequest req = MockHttpRequest.create("GET",
            "http://localhost/candlepin/status");

        interceptor.preProcess(req, rminvoker);

        PageRequest p = ResteasyProviderFactory.getContextData(PageRequest.class);
        assertNull(p);
    }

    @Test
    public void testBothLimitAndPage() throws Exception {
        MockHttpRequest req = MockHttpRequest.create("GET",
            "http://localhost/candlepin/status?per_page=10&page=4");

        interceptor.preProcess(req, rminvoker);

        PageRequest p = ResteasyProviderFactory.getContextData(PageRequest.class);
        assertEquals(Integer.valueOf(10), p.getPerPage());
        assertEquals(Integer.valueOf(4), p.getPage());
        assertEquals(PageRequest.DEFAULT_ORDER, p.getOrder());
        assertNull(p.getSortBy());
    }

    @Test
    public void testNoLimitButPage() throws Exception {
        MockHttpRequest req = MockHttpRequest.create("GET",
            "http://localhost/candlepin/status?page=5");

        interceptor.preProcess(req, rminvoker);

        PageRequest p = ResteasyProviderFactory.getContextData(PageRequest.class);
        assertEquals(PageRequest.DEFAULT_PER_PAGE, p.getPerPage());
        assertEquals(Integer.valueOf(5), p.getPage());
        assertEquals(PageRequest.DEFAULT_ORDER, p.getOrder());
        assertNull(p.getSortBy());
    }

    @Test
    public void testLimitButNoPage() throws Exception {
        MockHttpRequest req = MockHttpRequest.create("GET",
            "http://localhost/candlepin/status?per_page=10");

        interceptor.preProcess(req, rminvoker);

        PageRequest p = ResteasyProviderFactory.getContextData(PageRequest.class);
        assertEquals(Integer.valueOf(10), p.getPerPage());
        assertEquals(PageRequest.DEFAULT_PAGE, p.getPage());
        assertEquals(PageRequest.DEFAULT_ORDER, p.getOrder());
        assertNull(p.getSortBy());
    }

    @Test(expected = BadRequestException.class)
    public void testBadIntegerValue() throws Exception {
        MockHttpRequest req = MockHttpRequest.create("GET",
            "http://localhost/candlepin/status?page=foo&per_page=456");

        interceptor.preProcess(req, rminvoker);
    }

    @Test(expected = BadRequestException.class)
    public void testDoesNotAllowPageZero() throws Exception {
        MockHttpRequest req = MockHttpRequest.create("GET",
            "http://localhost/candlepin/status?page=0&per_page=456");

        interceptor.preProcess(req, rminvoker);
    }

    @Test
    public void testNoPagingIfJustOrderAndSortBy() throws Exception {
        MockHttpRequest req = MockHttpRequest.create("GET",
            "http://localhost/candlepin/status?order=asc&sort_by=id");

        interceptor.preProcess(req, rminvoker);

        PageRequest p = ResteasyProviderFactory.getContextData(PageRequest.class);
        assertFalse(p.isPaging());
        assertEquals(PageRequest.Order.ASCENDING, p.getOrder());
        assertEquals("id", p.getSortBy());
    }

    @Test
    public void testUsesDefaultOrderIfNoOrderProvided() throws Exception {
        MockHttpRequest req = MockHttpRequest.create("GET",
            "http://localhost/candlepin/status?sort_by=id");

        interceptor.preProcess(req, rminvoker);

        PageRequest p = ResteasyProviderFactory.getContextData(PageRequest.class);
        assertFalse(p.isPaging());
        assertEquals(PageRequest.DEFAULT_ORDER, p.getOrder());
        assertEquals("id", p.getSortBy());
    }

    @Test
    public void testDescendingOrder() throws Exception {
        MockHttpRequest req = MockHttpRequest.create("GET",
            "http://localhost/candlepin/status?order=descending&sort_by=id");

        interceptor.preProcess(req, rminvoker);

        PageRequest p = ResteasyProviderFactory.getContextData(PageRequest.class);
        assertFalse(p.isPaging());
        assertEquals(PageRequest.Order.DESCENDING, p.getOrder());
        assertEquals("id", p.getSortBy());
    }
}
