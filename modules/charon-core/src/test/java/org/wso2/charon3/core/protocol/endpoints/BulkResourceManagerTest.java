package org.wso2.charon3.core.protocol.endpoints;

import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.charon3.core.exceptions.AbstractCharonException;
import org.wso2.charon3.core.objects.User;
import org.wso2.charon3.core.objects.bulk.BulkResponseData;
import org.wso2.charon3.core.protocol.ResponseCodeConstants;
import org.wso2.charon3.core.protocol.SCIMResponse;
import org.wso2.charon3.core.schema.SCIMConstants;
import org.wso2.charon3.core.setup.CharonInitializer;
import org.wso2.charon3.core.testsetup.FileReferences;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 *
 * <br><br>
 * created at: 03.04.2019
 * @author Pascal Knüppel
 */
class BulkResourceManagerTest extends CharonInitializer implements FileReferences {

    private static final Logger log = LoggerFactory.getLogger(BulkResourceManagerTest.class);

    /**
     * the class that is going to be tested by handling bulk requests
     */
    private BulkResourceManager bulkResourceManager;

    @BeforeEach
    @Override
    public void initialize() {
        super.initialize();
        bulkResourceManager = new BulkResourceManager(createResourceManagers());
    }

    protected List<ResourceManager> createResourceManagers() {
        List<ResourceManager> resourceManagerList = new ArrayList<>();
        resourceManagerList.add(userManager);
        resourceManagerList.add(groupManager);
        return resourceManagerList;
    }

    /**
     * this method will show that the bulk request is handled correctly if a single bulk operation is executed
     */
    @Test
    public void handleBulkRequestWithOneOperation() throws AbstractCharonException {
        // this operation contains a single "create user" operation
        String bulkRequestOneOperation = readResourceFile(POST_BULK_REQUEST_FILE);
        SCIMResponse scimResponse = bulkResourceManager.processBulkData(bulkRequestOneOperation);

        Assertions.assertEquals(ResponseCodeConstants.CODE_OK, scimResponse.getResponseStatus());
        Mockito.verify(userResourceHandler, Mockito.times(1)).create(Mockito.any(), Mockito.any());

        log.trace(scimResponse.getResponseMessage());
        BulkResponseData bulkResponseData = JSON_DECODER.decodeBulkResponseData(scimResponse.getResponseMessage());
        Assertions.assertEquals(1, bulkResponseData.getOperationResponseList().size());
        bulkResponseData.getOperationResponseList().forEach(bulkResponseContent -> {
            Assertions.assertEquals(SCIMConstants.OperationalConstants.POST, bulkResponseContent.getMethod());
            MatcherAssert.assertThat(bulkResponseContent.getLocation(), Matchers.not(Matchers.blankOrNullString()));
            MatcherAssert.assertThat(bulkResponseContent.getLocation(),
                                     Matchers.startsWith(CharonInitializer.BASE_URI + SCIMConstants.USER_ENDPOINT));
            Assertions.assertEquals("qwerty", bulkResponseContent.getBulkID());

            Assertions.assertNotNull(bulkResponseContent.getScimResponse());
            Assertions.assertEquals(ResponseCodeConstants.CODE_CREATED,
                                    bulkResponseContent.getScimResponse().getResponseStatus());
            MatcherAssert.assertThat(bulkResponseContent.getScimResponse().getResponseMessage(),
                                     Matchers.blankOrNullString());
        });
    }

    /**
     * this method will show that the bulk request is handled correctly if a single bulk operation is executed
     */
    @Test
    public void handleBulkRequestWithTwoOperation() throws AbstractCharonException {
        // this operation contains a single "create user" operation
        String bulkRequestOneOperation = readResourceFile(USER_AND_GROUP_POST_BULK_REQUEST_FILE);
        SCIMResponse scimResponse = bulkResourceManager.processBulkData(bulkRequestOneOperation);

        Assertions.assertEquals(ResponseCodeConstants.CODE_OK, scimResponse.getResponseStatus());
        Mockito.verify(userResourceHandler, Mockito.times(1)).create(Mockito.any(), Mockito.any());
        Mockito.verify(groupResourceHandler, Mockito.times(1)).create(Mockito.any(), Mockito.any());

        log.trace(scimResponse.getResponseMessage());
        BulkResponseData bulkResponseData = JSON_DECODER.decodeBulkResponseData(scimResponse.getResponseMessage());
        Assertions.assertEquals(2, bulkResponseData.getOperationResponseList().size());
        bulkResponseData.getOperationResponseList().forEach(bulkResponseContent -> {
            Assertions.assertEquals(SCIMConstants.OperationalConstants.POST, bulkResponseContent.getMethod());
            MatcherAssert.assertThat(bulkResponseContent.getLocation(), Matchers.not(Matchers.blankOrNullString()));
            MatcherAssert.assertThat(bulkResponseContent.getLocation(),
                                     Matchers.startsWith(CharonInitializer.BASE_URI));
            MatcherAssert.assertThat(bulkResponseContent.getBulkID(), Matchers.not(Matchers.blankOrNullString()));

            Assertions.assertNotNull(bulkResponseContent.getScimResponse());
            Assertions.assertEquals(ResponseCodeConstants.CODE_CREATED,
                                    bulkResponseContent.getScimResponse().getResponseStatus());
            MatcherAssert.assertThat(bulkResponseContent.getScimResponse().getResponseMessage(),
                                     Matchers.blankOrNullString());
        });
    }

    /**
     * shows that the put-method is correctly called by a bulk operation
     */
    @Test
    public void handlePutBulkRequest() throws AbstractCharonException {
        User alice = new User();
        alice.replaceId(UUID.randomUUID().toString());
        alice.replaceUsername("Alice");
        alice.setSchemas();
        userResourceHandler.create(alice, null);

        // this operation contains a single "create user" operation
        String bulkRequestOneOperation = readResourceFile(PUT_BULK_REQUEST_FILE,
                                                          content -> content.replace("${userId}", alice.getId()));
        SCIMResponse scimResponse = bulkResourceManager.processBulkData(bulkRequestOneOperation);

        Assertions.assertEquals(ResponseCodeConstants.CODE_OK, scimResponse.getResponseStatus());
        Mockito.verify(userResourceHandler, Mockito.times(1)).update(Mockito.any(), Mockito.any());

        log.trace(scimResponse.getResponseMessage());
        BulkResponseData bulkResponseData = JSON_DECODER.decodeBulkResponseData(scimResponse.getResponseMessage());
        Assertions.assertEquals(1, bulkResponseData.getOperationResponseList().size());
        bulkResponseData.getOperationResponseList().forEach(bulkResponseContent -> {
            Assertions.assertEquals(SCIMConstants.OperationalConstants.PUT, bulkResponseContent.getMethod());
            MatcherAssert.assertThat(bulkResponseContent.getLocation(), Matchers.not(Matchers.blankOrNullString()));
            MatcherAssert.assertThat(bulkResponseContent.getLocation(),
                                     Matchers.startsWith(CharonInitializer.BASE_URI + SCIMConstants.USER_ENDPOINT));
            MatcherAssert.assertThat(bulkResponseContent.getBulkID(), Matchers.blankOrNullString());

            Assertions.assertNotNull(bulkResponseContent.getScimResponse());
            Assertions.assertEquals(ResponseCodeConstants.CODE_OK,
                                    bulkResponseContent.getScimResponse().getResponseStatus());
            MatcherAssert.assertThat(bulkResponseContent.getScimResponse().getResponseMessage(),
                                     Matchers.emptyString());
        });
    }

    /**
     * shows that the put-method is correctly called by a bulk operation
     */
    @Test
    public void handleDeleteBulkRequest() throws AbstractCharonException {
        User alice = new User();
        alice.replaceId(UUID.randomUUID().toString());
        alice.replaceUsername("Alice");
        alice.setSchemas();
        userResourceHandler.create(alice, null);

        // this operation contains a single "create user" operation
        String bulkRequestOneOperation = readResourceFile(DELETE_BULK_REQUEST_FILE,
                                                          content -> content.replace("${userId}", alice.getId()));
        SCIMResponse scimResponse = bulkResourceManager.processBulkData(bulkRequestOneOperation);

        Assertions.assertEquals(ResponseCodeConstants.CODE_OK, scimResponse.getResponseStatus());
        Mockito.verify(userResourceHandler, Mockito.times(1)).delete(Mockito.eq(alice.getId()));

        log.trace(scimResponse.getResponseMessage());
        BulkResponseData bulkResponseData = JSON_DECODER.decodeBulkResponseData(scimResponse.getResponseMessage());
        Assertions.assertEquals(1, bulkResponseData.getOperationResponseList().size());
        bulkResponseData.getOperationResponseList().forEach(bulkResponseContent -> {
            Assertions.assertEquals(SCIMConstants.OperationalConstants.DELETE, bulkResponseContent.getMethod());
            MatcherAssert.assertThat(bulkResponseContent.getLocation(), Matchers.blankOrNullString());
            MatcherAssert.assertThat(bulkResponseContent.getBulkID(), Matchers.blankOrNullString());

            Assertions.assertNotNull(bulkResponseContent.getScimResponse());
            Assertions.assertEquals(ResponseCodeConstants.CODE_NO_CONTENT,
                                    bulkResponseContent.getScimResponse().getResponseStatus());
            MatcherAssert.assertThat(bulkResponseContent.getScimResponse().getResponseMessage(),
                                     Matchers.emptyString());
        });
    }
}
