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
import org.apache.maven.shared.release.config.ReleaseDescriptor;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Test the SCM modification check phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class RewritePomsForReleasePhaseTest
    extends AbstractEditModeRewritingReleasePhaseTestCase
{
    private static final String NEXT_VERSION = "1.0";

    private static final String ALTERNATIVE_NEXT_VERSION = "2.0";

    protected void setUp()
        throws Exception
    {
        super.setUp();

        phase = (ReleasePhase) lookup( ReleasePhase.ROLE, "rewrite-poms-for-release" );
    }

    protected List createReactorProjects( String path, boolean copyFiles )
        throws Exception
    {
        return createReactorProjects( "rewrite-for-release/", path, copyFiles );
    }

    protected String readTestProjectFile( String fileName )
        throws IOException
    {
        return readXmlFile( getTestFile( "target/test-classes/projects/rewrite-for-release/" + fileName ) );
    }

    public void testSimulateRewrite()
        throws Exception
    {
        List reactorProjects = createReactorProjectsFromBasicPom();
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
        config.mapReleaseVersion( "groupId:artifactId", NEXT_VERSION );

        String expected = readTestProjectFile( "basic-pom/pom.xml" );

        phase.simulate( config, null, reactorProjects );

        String actual = readTestProjectFile( "basic-pom/pom.xml" );
        assertEquals( "Check the original POM untouched", expected, actual );

        expected = readTestProjectFile( "basic-pom/expected-pom.xml" );
        actual = readTestProjectFile( "basic-pom/pom.xml.tag" );
        assertEquals( "Check the transformed POM", expected, actual );
    }

    public void testClean()
        throws Exception
    {
        List reactorProjects = createReactorProjectsFromBasicPom();
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
        config.mapReleaseVersion( "groupId:artifactId", NEXT_VERSION );

        File testFile = getTestFile( "target/test-classes/projects/rewrite-for-release/basic-pom/pom.xml.tag" );
        testFile.delete();
        assertFalse( testFile.exists() );

        phase.simulate( config, null, reactorProjects );

        assertTrue( testFile.exists() );

        phase.clean( reactorProjects );

        assertFalse( testFile.exists() );
    }

    public void testCleanNotExists()
        throws Exception
    {
        List reactorProjects = createReactorProjectsFromBasicPom();
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
        config.mapReleaseVersion( "groupId:artifactId", NEXT_VERSION );

        File testFile = getTestFile( "target/test-classes/projects/rewrite-for-release/basic-pom/pom.xml.tag" );
        testFile.delete();
        assertFalse( testFile.exists() );

        phase.clean( reactorProjects );

        assertFalse( testFile.exists() );
    }

    //MRELEASE-116
    public void testScmOverridden()
        throws Exception
    {
        List reactorProjects = createReactorProjects( "pom-with-overridden-scm" );
        ReleaseDescriptor config = createConfigurationForWithParentNextVersion( reactorProjects );
        config.mapReleaseVersion( "groupId:subsubproject", NEXT_VERSION );

        phase.execute( config, null, reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    protected void mapAlternateNextVersion( ReleaseDescriptor config, String projectId )
    {
        config.mapReleaseVersion( projectId, ALTERNATIVE_NEXT_VERSION );
    }

    protected void mapNextVersion( ReleaseDescriptor config, String projectId )
    {
        config.mapReleaseVersion( projectId, NEXT_VERSION );
    }

    protected ReleaseDescriptor createConfigurationForPomWithParentAlternateNextVersion( List reactorProjects )
        throws Exception
    {
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );

        config.mapReleaseVersion( "groupId:artifactId", NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject1", ALTERNATIVE_NEXT_VERSION );
        return config;
    }

    protected ReleaseDescriptor createConfigurationForWithParentNextVersion( List reactorProjects )
        throws Exception
    {
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );

        config.mapReleaseVersion( "groupId:artifactId", NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject1", NEXT_VERSION );
        return config;
    }

    protected void unmapNextVersion( ReleaseDescriptor config, String projectId )
    {
        // nothing to do
    }

    public void testRewriteBasicPomWithCvs()
        throws Exception
    {

        List reactorProjects = createReactorProjects( "basic-pom-with-cvs" );
        ReleaseDescriptor config = this.createDescriptorFromProjects( reactorProjects );
        mapNextVersion( config, "groupId:artifactId" );

        phase.execute( config, null, reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewriteBasicPomWithTagBase()
        throws Exception
    {

        List reactorProjects = createReactorProjects( "basic-pom-with-tag-base" );
        ReleaseDescriptor config = this.createDescriptorFromProjects( reactorProjects );
        config.setScmTagBase( "file://localhost/tmp/scm-repo/releases" );
        mapNextVersion( config, "groupId:artifactId" );

        phase.execute( config, null, reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewriteBasicPomWithTagBaseAndVaryingScmUrls()
        throws Exception
    {
        List reactorProjects = createReactorProjects( "basic-pom-with-tag-base-and-varying-scm-urls" );
        ReleaseDescriptor config = this.createDescriptorFromProjects( reactorProjects );
        config.setScmTagBase( "file://localhost/tmp/scm-repo/allprojects/releases" );
        mapNextVersion( config, "groupId:artifactId" );

        phase.execute( config, null, reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewriteBasicPomWithCvsFromTag()
        throws Exception
    {
        List reactorProjects = createReactorProjects( "basic-pom-with-cvs-from-tag" );
        ReleaseDescriptor config = this.createDescriptorFromProjects( reactorProjects );
        mapNextVersion( config, "groupId:artifactId" );

        phase.execute( config, null, reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewriteBasicPomWithEmptyScm()
        throws Exception
    {
        List reactorProjects = createReactorProjects( "basic-pom-with-empty-scm" );
        ReleaseDescriptor config = this.createDescriptorFromProjects( reactorProjects );
        mapNextVersion( config, "groupId:artifactId" );

        phase.execute( config, null, reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewriteInterpolatedVersions()
        throws Exception
    {
        List reactorProjects = createReactorProjects( "interpolated-versions" );
        ReleaseDescriptor config = createMappedConfiguration( reactorProjects );

        phase.execute( config, null, reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewriteInterpolatedVersionsDifferentVersion()
        throws Exception
    {

        List reactorProjects = createReactorProjects( "interpolated-versions" );
        ReleaseDescriptor config = this.createDescriptorFromProjects( reactorProjects );

        config.mapReleaseVersion( "groupId:artifactId", NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject1", ALTERNATIVE_NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject2", NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject3", NEXT_VERSION );

        phase.execute( config, null, reactorProjects );

        for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
        {
            MavenProject project = (MavenProject) i.next();

            // skip subproject1 - we don't need to worry about its version mapping change, it has no deps of any kind
            if ( !"groupId".equals( project.getGroupId() ) || !"subproject1".equals( project.getArtifactId() ) )
            {
                comparePomFiles( project, "-different-version" );
            }
        }
    }

    public void testRewriteBasicPomWithInheritedScm()
        throws Exception
    {
        List reactorProjects = createReactorProjects( "basic-pom-inherited-scm" );
        ReleaseDescriptor config = createConfigurationForWithParentNextVersion( reactorProjects );
        config.mapReleaseVersion( "groupId:subsubproject", NEXT_VERSION );

        phase.execute( config, null, reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    public void testRewritePomWithParentAndProperties()
        throws Exception
    {
        List reactorProjects = createReactorProjects( "pom-with-parent-and-properties" );

        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );
        config.mapReleaseVersion( "groupId:artifactId", NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject1", ALTERNATIVE_NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject2", ALTERNATIVE_NEXT_VERSION );

        phase.execute( config, null, reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    protected ReleaseDescriptor createDescriptorFromProjects( List reactorProjects )
    {
        ReleaseDescriptor descriptor = super.createDescriptorFromProjects( reactorProjects );
        descriptor.setScmReleaseLabel( "release-label" );
        return descriptor;
    }
}
