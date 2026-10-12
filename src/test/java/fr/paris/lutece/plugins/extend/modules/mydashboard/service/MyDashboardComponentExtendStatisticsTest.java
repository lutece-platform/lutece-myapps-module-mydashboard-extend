/*
 * Copyright (c) 2002-2026, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.extend.modules.mydashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.extend.service.extender.history.IResourceExtenderHistoryService;
import fr.paris.lutece.plugins.extend.service.extender.history.ResourceExtenderHistoryService;
import fr.paris.lutece.plugins.mydashboard.service.IMyDashboardComponent;
import fr.paris.lutece.portal.service.security.LuteceAuthentication;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.SecurityService;
import fr.paris.lutece.test.LuteceTestCase;
import fr.paris.lutece.test.mocks.MockHttpServletRequest;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Tests of the dashboard component counting the extender actions of a user.
 */
public class MyDashboardComponentExtendStatisticsTest extends LuteceTestCase
{
    private static final String COMPONENT_ID = "extend.myDashboardComponentExtenderStatistics";
    private static final String USER_GUID = "unit_test_mydashboard_user";
    private static final String OTHER_USER_GUID = "unit_test_mydashboard_other";
    private static final String RESOURCE_TYPE = "unit_test_mydashboard";
    private static final String TYPE_COMMENT = "unit_test_comment";
    private static final String TYPE_RATING = "unit_test_rating";

    @Inject
    private Instance<IMyDashboardComponent> _dashboardComponents;

    @Inject
    @Named( ResourceExtenderHistoryService.BEAN_SERVICE )
    private IResourceExtenderHistoryService _resourceExtenderHistoryService;

    /**
     * Removes the history rows the tests created.
     */
    @AfterEach
    public void removeHistory( )
    {
        _resourceExtenderHistoryService.removeByResource( TYPE_COMMENT, "1", RESOURCE_TYPE );
        _resourceExtenderHistoryService.removeByResource( TYPE_COMMENT, "2", RESOURCE_TYPE );
        _resourceExtenderHistoryService.removeByResource( TYPE_RATING, "1", RESOURCE_TYPE );
    }

    /**
     * The component is discovered as a mydashboard component, with its id and its localized description.
     */
    @Test
    public void testComponentDiscovered( )
    {
        IMyDashboardComponent component = getComponent( );

        assertInstanceOf( MyDashboardComponentExtendStatistics.class, component, "component class" );
        assertEquals( COMPONENT_ID, component.getComponentId( ), "component id" );
        assertEquals( "Mes participations", component.getComponentDescription( Locale.FRENCH ), "french description" );
        assertEquals( "My participations", component.getComponentDescription( Locale.ENGLISH ), "english description" );
    }

    /**
     * An anonymous visitor gets no content.
     */
    @Test
    public void testAnonymousGetsNothing( )
    {
        assertEquals( "", getComponent( ).getDashboardData( new MockHttpServletRequest( ) ), "anonymous content" );
    }

    /**
     * A signed-in user gets the count of his own actions per extender type, never those of another user.
     */
    @Test
    public void testCountsPerExtenderType( )
    {
        _resourceExtenderHistoryService.create( TYPE_COMMENT, "1", RESOURCE_TYPE, USER_GUID );
        _resourceExtenderHistoryService.create( TYPE_COMMENT, "2", RESOURCE_TYPE, USER_GUID );
        _resourceExtenderHistoryService.create( TYPE_RATING, "1", RESOURCE_TYPE, USER_GUID );
        _resourceExtenderHistoryService.create( TYPE_RATING, "1", RESOURCE_TYPE, OTHER_USER_GUID );

        MockHttpServletRequest request = new MockHttpServletRequest( );
        SecurityService.getInstance( ).registerUser( request, new TestUser( USER_GUID ) );

        String strText = getComponent( ).getDashboardData( request ).replaceAll( "<[^>]+>", " " ).replaceAll( "\\s+", " " );

        assertTrue( strText.contains( " " + TYPE_COMMENT + " 2 " ), "comment count: " + strText );
        assertTrue( strText.contains( " " + TYPE_RATING + " 1 " ), "rating count: " + strText );
    }

    /**
     * A signed-in user without any action gets the empty state instead of a table.
     */
    @Test
    public void testEmptyState( )
    {
        MockHttpServletRequest request = new MockHttpServletRequest( );
        SecurityService.getInstance( ).registerUser( request, new TestUser( OTHER_USER_GUID + "_none" ) );

        String strHtml = getComponent( ).getDashboardData( request );

        assertTrue( strHtml.contains( "lutece-ds-empty" ), "empty state: " + strHtml );
        assertFalse( strHtml.contains( "<table" ), "no table: " + strHtml );
    }

    /**
     * Returns the component of this module among the discovered dashboard components.
     *
     * @return the component
     */
    private IMyDashboardComponent getComponent( )
    {
        List<IMyDashboardComponent> listComponents = _dashboardComponents.stream( ).filter( c -> COMPONENT_ID.equals( c.getComponentId( ) ) ).toList( );
        assertEquals( 1, listComponents.size( ), "components with the module id" );

        return listComponents.get( 0 );
    }

    /**
     * Front-office user of the tests.
     */
    private static final class TestUser extends LuteceUser
    {
        private static final long serialVersionUID = 1L;

        /**
         * Creates a user of a stub authentication service.
         *
         * @param strName
         *            the user name
         */
        TestUser( String strName )
        {
            super( strName, (LuteceAuthentication) Proxy.newProxyInstance( LuteceAuthentication.class.getClassLoader( ),
                    new Class<?> [ ] { LuteceAuthentication.class }, ( proxy, method, args ) -> "unit_test" ) );
        }
    }
}
