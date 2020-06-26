/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.cloud.storage;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobContainerItem;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.file.share.ShareClient;
import com.azure.storage.file.share.ShareServiceClient;
import com.azure.storage.file.share.models.ShareFileItem;
import com.azure.storage.file.share.models.ShareItem;
import static com.microsoft.azure.spring.cloud.storage.AzureStorageUtils.isAzureStorageResource;
import static com.microsoft.azure.spring.cloud.storage.StorageType.BLOB;
import static com.microsoft.azure.spring.cloud.storage.StorageType.FILE;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.AntPathMatcher;

/**
 * An Azure Storage specific ResourcePatternResolver.
 */
public class AzureStorageResourcePatternResolver implements ResourcePatternResolver {

    /**
     * Stores the Ant path matcher.
     */
    private final AntPathMatcher matcher = new AntPathMatcher();

    /**
     * Stores the BlobServiceClient.
     */
    private BlobServiceClient blobServiceClient;

    /**
     * Stores the ShareServiceClient shareServiceClient
     */
    private ShareServiceClient shareServiceClient;

    /**
     * Constructor.
     *
     * @param blobServiceClient the BlobServiceClient.
     */
    public AzureStorageResourcePatternResolver(BlobServiceClient blobServiceClient) {
        this(blobServiceClient, null);
    }

    /**
     * Constructor.
     *
     * @param shareServiceClient the ShareServiceClient.
     */
    public AzureStorageResourcePatternResolver(ShareServiceClient shareServiceClient) {
        this(null, shareServiceClient);
    }

    /**
     * Constructor.
     *
     * @param blobServiceClient the BlobServiceClient.
     * @param shareServiceClient the ShareServiceClient.
     */
    public AzureStorageResourcePatternResolver(
            BlobServiceClient blobServiceClient, ShareServiceClient shareServiceClient) {
        this.blobServiceClient = blobServiceClient;
        this.shareServiceClient = shareServiceClient;
    }

    /**
     * @see ResourcePatternResolver#getResources(java.lang.String)
     */
    @Override
    public Resource[] getResources(String pattern) throws IOException {
        Resource[] resources = null;

        if (isAzureStorageResource(pattern, BLOB)) {
            resources = getBlobResources(pattern);
        } else if (isAzureStorageResource(pattern, FILE)) {
            resources = getShareResources(pattern);
        }

        return resources;
    }

    /**
     * @see ResourcePatternResolver#getResource(java.lang.String)
     */
    @Override
    public Resource getResource(String location) {
        Resource resource = null;

        if (isAzureStorageResource(location, BLOB) && blobServiceClient != null) {
            resource = new BlobStorageResource(blobServiceClient, location, true);
        } else if (isAzureStorageResource(location, FILE) && shareServiceClient != null) {
            resource = new FileStorageResource(shareServiceClient, location, true);
        }

        return resource;
    }

    /**
     * @see ResourcePatternResolver#getClassLoader()
     */
    @Override
    public ClassLoader getClassLoader() {
        return getClass().getClassLoader();
    }

    /**
     * Get the blob resources for the given pattern.
     *
     * @param pattern the pattern.
     * @return the blob resources.
     */
    private Resource[] getBlobResources(String pattern) {
        ArrayList<Resource> resources = new ArrayList<>();
        Iterator<BlobContainerItem> containerIterator
                = blobServiceClient.listBlobContainers().iterator();
        while (containerIterator.hasNext()) {
            BlobContainerItem containerItem = containerIterator.next();
            String containerName = containerItem.getName();
            BlobContainerClient blobContainerClient
                    = blobServiceClient.getBlobContainerClient(containerItem.getName());
            Iterator<BlobItem> blobIterator = blobContainerClient.listBlobs().iterator();
            while (blobIterator.hasNext()) {
                BlobItem blobItem = blobIterator.next();
                String blobName = blobItem.getName();
                String location = "azure-blob://" + containerName + "/" + blobName;
                if (matcher.match(pattern, location)) {
                    resources.add(new BlobStorageResource(blobServiceClient, location));
                }
            }
        }
        return resources.toArray(new Resource[]{});
    }

    /**
     * Get the share resources for the given pattern.
     *
     * @param pattern the pattern.
     * @return the share resources.
     */
    private Resource[] getShareResources(String pattern) {
        ArrayList<Resource> resources = new ArrayList<>();
        Iterator<ShareItem> shareIterator
                = shareServiceClient.listShares().iterator();
        while (shareIterator.hasNext()) {
            ShareItem shareItem = shareIterator.next();
            String shareName = shareItem.getName();
            ShareClient shareClient
                    = shareServiceClient.getShareClient(shareItem.getName());
            Iterator<ShareFileItem> shareFileIterator = shareClient
                    .getRootDirectoryClient().listFilesAndDirectories().iterator();
            while (shareFileIterator.hasNext()) {
                ShareFileItem fileItem = shareFileIterator.next();
                String filename = fileItem.getName();
                if (!fileItem.isDirectory()) {
                    String location = "azure-file://" + shareName + "/" + filename;
                    if (matcher.match(pattern, location)) {
                        resources.add(new FileStorageResource(shareServiceClient, location));
                    }
                }
            }
        }
        return resources.toArray(new Resource[]{});
    }
}
