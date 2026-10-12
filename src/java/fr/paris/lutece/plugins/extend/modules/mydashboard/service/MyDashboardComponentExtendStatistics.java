/*
 * Copyright (c) 2002-2022, City of Paris
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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import fr.paris.lutece.plugins.extend.business.extender.history.ResourceExtenderHistory;
import fr.paris.lutece.plugins.extend.business.extender.history.ResourceExtenderHistoryFilter;
import fr.paris.lutece.plugins.extend.service.extender.IResourceExtenderService;
import fr.paris.lutece.plugins.extend.service.extender.history.IResourceExtenderHistoryService;
import fr.paris.lutece.plugins.extend.service.extender.history.ResourceExtenderHistoryService;
import fr.paris.lutece.plugins.mydashboard.service.MyDashboardComponent;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.SecurityService;
import fr.paris.lutece.portal.service.template.AppTemplateService;
import fr.paris.lutece.util.ReferenceItem;
import fr.paris.lutece.util.html.HtmlTemplate;

/**
 * Dashboard component counting the extender actions of the current user by extender type.
 */
@ApplicationScoped
@Named( "extend-mydashboard.myDashboardComponentExtenderStatistics" )
public class MyDashboardComponentExtendStatistics extends MyDashboardComponent
{

	/**
	 * 
	 */
    private static final long serialVersionUID = 2516946268291209327L;

    private static final String TEMPLATE_VIEW_EXTEND_STATISTICS_LIST        = "skin/plugins/extend/mydashboard/dashboard_extend_statistics.html";
    private static final String MARK_EXTEND_STATISTICS_LIST      = "extend_statistics_list";
    private static final String MARK_EXTENDER_TYPES              = "extender_types";
    
    private static final String DASHBOARD_COMPONENT_ID           = "extend.myDashboardComponentExtenderStatistics";
    private static final String MESSAGE_COMPONENT_DESCRIPTION    = "module.extend.mydashboard.myDashboardComponentExtenderStatistics.description";

    @Inject
    @Named( ResourceExtenderHistoryService.BEAN_SERVICE )
    private IResourceExtenderHistoryService _resourceExtenderHistoryService;

    @Inject
    private IResourceExtenderService _resourceExtenderService;


    /**
     * {@inheritDoc}
     */
	@Override
	public String getDashboardData( HttpServletRequest request )
	{
        LuteceUser user = SecurityService.getInstance( ).getRegisteredUser( request );
        if ( user != null )
        {
            Map<String, Object> model = new HashMap< >( );
            
            ResourceExtenderHistoryFilter filter = new ResourceExtenderHistoryFilter( );
            filter.setUserGuid( user.getName( ) );

            Map<String, Integer> mapExtendStatistics = new TreeMap< >( );
            
            for ( ResourceExtenderHistory resourceExtenderHistory :  _resourceExtenderHistoryService.findByFilter( filter ) )
            {
            	Integer value = mapExtendStatistics.get( resourceExtenderHistory.getExtenderType( ) );
            	
            	if ( value != null && value > 0 )
            	{
            		mapExtendStatistics.put( resourceExtenderHistory.getExtenderType( ), value + 1 );
            	}
            	else 
            	{
            		mapExtendStatistics.put( resourceExtenderHistory.getExtenderType( ), 1 );
            	}
            }
            
            Map<String, String> mapExtenderTypes = new HashMap< >( );

            for ( ReferenceItem extenderType : _resourceExtenderService.getExtenderTypes( request.getLocale( ) ) )
            {
                if ( StringUtils.isNotBlank( extenderType.getName( ) ) )
                {
                    mapExtenderTypes.put( extenderType.getCode( ), extenderType.getName( ) );
                }
            }

            model.put( MARK_EXTEND_STATISTICS_LIST, mapExtendStatistics );
            model.put( MARK_EXTENDER_TYPES, mapExtenderTypes );
            
            HtmlTemplate htmlTemplate = AppTemplateService.getTemplate( TEMPLATE_VIEW_EXTEND_STATISTICS_LIST, request.getLocale( ), model );
            
            return htmlTemplate.getHtml( );
    	}
        
        return StringUtils.EMPTY;
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public String getComponentDescription( Locale locale )
	{
        return I18nService.getLocalizedString( MESSAGE_COMPONENT_DESCRIPTION, locale );
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public String getComponentId( )
	{
        return DASHBOARD_COMPONENT_ID;
	}

}
