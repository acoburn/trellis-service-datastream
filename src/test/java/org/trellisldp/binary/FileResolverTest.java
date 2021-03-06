/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.trellisldp.binary;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.File;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.trellisldp.api.BinaryService.Resolver;

/**
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public class FileResolverTest {

    private final static String testDoc = "test.txt";

    private final static String partition = "partition";

    private final static RDF rdf = new SimpleRDF();

    private final static String directory = new File(FileResolver.class.getResource("/" + testDoc).getPath())
        .getParent();

    private final static IRI file = rdf.createIRI("file:" + testDoc);

    private final static Map<String, String> partitions = new HashMap<>();

    @Mock
    private InputStream mockInputStream;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        partitions.clear();
        partitions.put(partition, directory);
    }

    @Test
    public void testFileExists() {
        final Resolver resolver = new FileResolver(partitions);
        assertTrue(resolver.exists(partition, file));
        assertFalse(resolver.exists(partition, rdf.createIRI("file:fake.txt")));
    }

    @Test
    public void testFilePurge() {
        final Resolver resolver = new FileResolver(partitions);
        final IRI fileIRI = rdf.createIRI("file:" + randomFilename());
        final InputStream inputStream = new ByteArrayInputStream("Some data".getBytes(UTF_8));
        resolver.setContent(partition, fileIRI, inputStream);
        assertTrue(resolver.exists(partition, fileIRI));
        resolver.purgeContent(partition, fileIRI);
        assertFalse(resolver.exists(partition, fileIRI));

    }

    @Test
    public void testFileContent() {
        final Resolver resolver = new FileResolver(partitions);
        assertTrue(resolver.getContent(partition, file).isPresent());
        assertEquals("A test document.\n", resolver.getContent(partition, file).map(this::uncheckedToString).get());
    }

    @Test
    public void testSetFileContent() {
        final String contents = "A new file";
        final Resolver resolver = new FileResolver(partitions);
        final IRI fileIRI = rdf.createIRI("file:" + randomFilename());
        final InputStream inputStream = new ByteArrayInputStream(contents.getBytes(UTF_8));
        resolver.setContent(partition, fileIRI, inputStream);
        assertTrue(resolver.getContent(partition, fileIRI).isPresent());
        assertEquals(contents, resolver.getContent(partition, fileIRI).map(this::uncheckedToString).get());
    }

    @Test
    public void testGetFileContentError() throws IOException {
        final Resolver resolver = new FileResolver(partitions);
        final IRI fileIRI = rdf.createIRI("file:" + randomFilename());
        assertThrows(UncheckedIOException.class, () -> resolver.getContent(partition, fileIRI));
    }

    @Test
    public void testSetFileContentError() throws IOException {
        when(mockInputStream.read(any(byte[].class))).thenThrow(new IOException("Expected error"));
        final Resolver resolver = new FileResolver(partitions);
        final IRI fileIRI = rdf.createIRI("file:" + randomFilename());
        assertThrows(UncheckedIOException.class, () -> resolver.setContent(partition, fileIRI, mockInputStream));
    }

    @Test
    public void testFileSchemes() {
        final Resolver resolver = new FileResolver(partitions);
        assertEquals(1L, resolver.getUriSchemes().size());
        assertTrue(resolver.getUriSchemes().contains("file"));
    }

    @Test
    public void testMultipart() {
        final Resolver resolver = new FileResolver(partitions);
        assertFalse(resolver.supportsMultipartUpload());
    }

    @Test
    public void testMultipartAbort() {
        final Resolver resolver = new FileResolver(partitions);
        assertThrows(UnsupportedOperationException.class, () -> resolver.abortUpload("test-identifier"));
    }

    @Test
    public void testMultipartComplete() {
        final Resolver resolver = new FileResolver(partitions);
        assertThrows(UnsupportedOperationException.class, () -> resolver.completeUpload("test-identifier", emptyMap()));
    }

    @Test
    public void testListParts() {
        final Resolver resolver = new FileResolver(partitions);
        assertThrows(UnsupportedOperationException.class, () -> resolver.listParts("foo"));
    }

    @Test
    public void testMultipartUpload() {
        final String contents = "A new resource";
        final InputStream inputStream = new ByteArrayInputStream(contents.getBytes(UTF_8));
        final Resolver resolver = new FileResolver(partitions);
        assertThrows(UnsupportedOperationException.class, () -> resolver.uploadPart("test-identifier", 1, inputStream));
    }

    @Test
    public void testMultipartInitiate() {
        final Resolver resolver = new FileResolver(partitions);
        assertThrows(UnsupportedOperationException.class, () -> resolver.initiateUpload(partition, file, "text/plain"));
    }

    @Test
    public void testMultipartIdentifierExists() {
        final Resolver resolver = new FileResolver(partitions);
        assertThrows(UnsupportedOperationException.class, () -> resolver.uploadSessionExists("test-identifier"));
    }

    private String uncheckedToString(final InputStream is) {
        try {
            return IOUtils.toString(is, UTF_8);
        } catch (final IOException ex) {
            return null;
        }
    }

    private static String randomFilename() {
        final SecureRandom random = new SecureRandom();
        final String filename = new BigInteger(50, random).toString(32);
        return filename + ".json";
    }
}
