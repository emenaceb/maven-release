package org.apache.maven.shared.release.phase;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmVersion;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.scm.ReleaseScmCommandException;
import org.apache.maven.shared.release.scm.ReleaseScmRepositoryException;
import org.apache.maven.shared.release.scm.ScmRepositoryConfigurator;
import org.apache.maven.shared.release.util.ReleaseUtil;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Commit the project to the SCM.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ScmCommitPhase
    extends AbstractReleasePhase
{
    /**
     * Tool that gets a configured SCM repository from release configuration.
     */
    private ScmRepositoryConfigurator scmRepositoryConfigurator;

    /**
     * The format for the commit message.
     */
    private String messageFormat;

    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseResult relResult = new ReleaseResult();

        validateConfiguration( releaseDescriptor );

        getLogger().info( "Checking in modified POMs..." );

        ScmRepository repository;
        ScmProvider provider;
        try
        {
            repository = scmRepositoryConfigurator.getConfiguredRepository( releaseDescriptor, settings );

            provider = scmRepositoryConfigurator.getRepositoryProvider( repository );
        }
        catch ( ScmRepositoryException e )
        {
            throw new ReleaseScmRepositoryException( e.getMessage(), e.getValidationMessages() );
        }
        catch ( NoSuchScmProviderException e )
        {
            throw new ReleaseExecutionException( "Unable to configure SCM repository: " + e.getMessage(), e );
        }

        if ( releaseDescriptor.isCommitByProject() )
        {
            for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
            {
                MavenProject project = (MavenProject) i.next();

                List pomFiles = createPomFiles( releaseDescriptor, project );
                ScmFileSet fileSet = new ScmFileSet( project.getFile().getParentFile(), pomFiles );

                checkin( provider, repository, fileSet, createMessage( releaseDescriptor ) );
            }
        }
        else
        {
            List pomFiles = createPomFiles( releaseDescriptor, reactorProjects );
            ScmFileSet fileSet = new ScmFileSet( new File( releaseDescriptor.getWorkingDirectory() ), pomFiles );

            checkin( provider, repository, fileSet, createMessage( releaseDescriptor ) );
        }

        relResult.setResultCode( ReleaseResult.SUCCESS );

        return relResult;
    }

    private void checkin( ScmProvider provider, ScmRepository repository, ScmFileSet fileSet, String message )
        throws ReleaseExecutionException, ReleaseScmCommandException
    {
        CheckInScmResult result;

        try
        {
            result = provider.checkIn( repository, fileSet, (ScmVersion) null, message );
        }
        catch ( ScmException e )
        {
            throw new ReleaseExecutionException( "An error is occurred in the checkin process: " + e.getMessage(), e );
        }

        if ( !result.isSuccess() )
        {
            throw new ReleaseScmCommandException( "Unable to commit files", result );
        }
    }

    public ReleaseResult simulate( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseResult result = new ReleaseResult();

        validateConfiguration( releaseDescriptor );

        Collection pomFiles = createPomFiles( releaseDescriptor, reactorProjects );
        logInfo( result, "Full run would be checking in " + pomFiles.size() + " files with message: '" +
            createMessage( releaseDescriptor ) + "'" );

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    private static void validateConfiguration( ReleaseDescriptor releaseDescriptor )
        throws ReleaseFailureException
    {
        if ( releaseDescriptor.getScmReleaseLabel() == null )
        {
            throw new ReleaseFailureException( "A release label is required for committing" );
        }
    }

    private String createMessage( ReleaseDescriptor releaseDescriptor )
    {
        return MessageFormat.format( releaseDescriptor.getScmCommentPrefix() + messageFormat,
                                     new Object[]{releaseDescriptor.getScmReleaseLabel()} );
    }

    private static List createPomFiles( ReleaseDescriptor releaseDescriptor, MavenProject project )
    {
        List pomFiles = new ArrayList();

        pomFiles.add( ReleaseUtil.getStandardPom( project ) );

        if ( releaseDescriptor.isGenerateReleasePoms() )
        {
            pomFiles.add( ReleaseUtil.getReleasePom( project ) );
        }

        return pomFiles;
    }

    private static List createPomFiles( ReleaseDescriptor releaseDescriptor, List reactorProjects )
    {
        List pomFiles = new ArrayList();
        for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
        {
            MavenProject project = (MavenProject) i.next();
            pomFiles.addAll( createPomFiles( releaseDescriptor, project ) );
        }
        return pomFiles;
    }
}
