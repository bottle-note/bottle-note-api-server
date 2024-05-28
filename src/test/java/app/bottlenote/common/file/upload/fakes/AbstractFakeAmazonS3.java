package app.bottlenote.common.file.upload.fakes;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.S3ResponseMetadata;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.BucketAccelerateConfiguration;
import com.amazonaws.services.s3.model.BucketCrossOriginConfiguration;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.BucketLoggingConfiguration;
import com.amazonaws.services.s3.model.BucketNotificationConfiguration;
import com.amazonaws.services.s3.model.BucketPolicy;
import com.amazonaws.services.s3.model.BucketReplicationConfiguration;
import com.amazonaws.services.s3.model.BucketTaggingConfiguration;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.BucketWebsiteConfiguration;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.CopyObjectResult;
import com.amazonaws.services.s3.model.CopyPartRequest;
import com.amazonaws.services.s3.model.CopyPartResult;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.DeleteBucketAnalyticsConfigurationRequest;
import com.amazonaws.services.s3.model.DeleteBucketAnalyticsConfigurationResult;
import com.amazonaws.services.s3.model.DeleteBucketCrossOriginConfigurationRequest;
import com.amazonaws.services.s3.model.DeleteBucketEncryptionRequest;
import com.amazonaws.services.s3.model.DeleteBucketEncryptionResult;
import com.amazonaws.services.s3.model.DeleteBucketIntelligentTieringConfigurationRequest;
import com.amazonaws.services.s3.model.DeleteBucketIntelligentTieringConfigurationResult;
import com.amazonaws.services.s3.model.DeleteBucketInventoryConfigurationRequest;
import com.amazonaws.services.s3.model.DeleteBucketInventoryConfigurationResult;
import com.amazonaws.services.s3.model.DeleteBucketLifecycleConfigurationRequest;
import com.amazonaws.services.s3.model.DeleteBucketMetricsConfigurationRequest;
import com.amazonaws.services.s3.model.DeleteBucketMetricsConfigurationResult;
import com.amazonaws.services.s3.model.DeleteBucketOwnershipControlsRequest;
import com.amazonaws.services.s3.model.DeleteBucketOwnershipControlsResult;
import com.amazonaws.services.s3.model.DeleteBucketPolicyRequest;
import com.amazonaws.services.s3.model.DeleteBucketReplicationConfigurationRequest;
import com.amazonaws.services.s3.model.DeleteBucketRequest;
import com.amazonaws.services.s3.model.DeleteBucketTaggingConfigurationRequest;
import com.amazonaws.services.s3.model.DeleteBucketWebsiteConfigurationRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectTaggingRequest;
import com.amazonaws.services.s3.model.DeleteObjectTaggingResult;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.DeletePublicAccessBlockRequest;
import com.amazonaws.services.s3.model.DeletePublicAccessBlockResult;
import com.amazonaws.services.s3.model.DeleteVersionRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetBucketAccelerateConfigurationRequest;
import com.amazonaws.services.s3.model.GetBucketAclRequest;
import com.amazonaws.services.s3.model.GetBucketAnalyticsConfigurationRequest;
import com.amazonaws.services.s3.model.GetBucketAnalyticsConfigurationResult;
import com.amazonaws.services.s3.model.GetBucketCrossOriginConfigurationRequest;
import com.amazonaws.services.s3.model.GetBucketEncryptionRequest;
import com.amazonaws.services.s3.model.GetBucketEncryptionResult;
import com.amazonaws.services.s3.model.GetBucketIntelligentTieringConfigurationRequest;
import com.amazonaws.services.s3.model.GetBucketIntelligentTieringConfigurationResult;
import com.amazonaws.services.s3.model.GetBucketInventoryConfigurationRequest;
import com.amazonaws.services.s3.model.GetBucketInventoryConfigurationResult;
import com.amazonaws.services.s3.model.GetBucketLifecycleConfigurationRequest;
import com.amazonaws.services.s3.model.GetBucketLocationRequest;
import com.amazonaws.services.s3.model.GetBucketLoggingConfigurationRequest;
import com.amazonaws.services.s3.model.GetBucketMetricsConfigurationRequest;
import com.amazonaws.services.s3.model.GetBucketMetricsConfigurationResult;
import com.amazonaws.services.s3.model.GetBucketNotificationConfigurationRequest;
import com.amazonaws.services.s3.model.GetBucketOwnershipControlsRequest;
import com.amazonaws.services.s3.model.GetBucketOwnershipControlsResult;
import com.amazonaws.services.s3.model.GetBucketPolicyRequest;
import com.amazonaws.services.s3.model.GetBucketPolicyStatusRequest;
import com.amazonaws.services.s3.model.GetBucketPolicyStatusResult;
import com.amazonaws.services.s3.model.GetBucketReplicationConfigurationRequest;
import com.amazonaws.services.s3.model.GetBucketTaggingConfigurationRequest;
import com.amazonaws.services.s3.model.GetBucketVersioningConfigurationRequest;
import com.amazonaws.services.s3.model.GetBucketWebsiteConfigurationRequest;
import com.amazonaws.services.s3.model.GetObjectAclRequest;
import com.amazonaws.services.s3.model.GetObjectLegalHoldRequest;
import com.amazonaws.services.s3.model.GetObjectLegalHoldResult;
import com.amazonaws.services.s3.model.GetObjectLockConfigurationRequest;
import com.amazonaws.services.s3.model.GetObjectLockConfigurationResult;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRetentionRequest;
import com.amazonaws.services.s3.model.GetObjectRetentionResult;
import com.amazonaws.services.s3.model.GetObjectTaggingRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingResult;
import com.amazonaws.services.s3.model.GetPublicAccessBlockRequest;
import com.amazonaws.services.s3.model.GetPublicAccessBlockResult;
import com.amazonaws.services.s3.model.GetS3AccountOwnerRequest;
import com.amazonaws.services.s3.model.HeadBucketRequest;
import com.amazonaws.services.s3.model.HeadBucketResult;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ListBucketAnalyticsConfigurationsRequest;
import com.amazonaws.services.s3.model.ListBucketAnalyticsConfigurationsResult;
import com.amazonaws.services.s3.model.ListBucketIntelligentTieringConfigurationsRequest;
import com.amazonaws.services.s3.model.ListBucketIntelligentTieringConfigurationsResult;
import com.amazonaws.services.s3.model.ListBucketInventoryConfigurationsRequest;
import com.amazonaws.services.s3.model.ListBucketInventoryConfigurationsResult;
import com.amazonaws.services.s3.model.ListBucketMetricsConfigurationsRequest;
import com.amazonaws.services.s3.model.ListBucketMetricsConfigurationsResult;
import com.amazonaws.services.s3.model.ListBucketsRequest;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.ListNextBatchOfObjectsRequest;
import com.amazonaws.services.s3.model.ListNextBatchOfVersionsRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ListPartsRequest;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.MultipartUploadListing;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.Owner;
import com.amazonaws.services.s3.model.PartListing;
import com.amazonaws.services.s3.model.PresignedUrlDownloadRequest;
import com.amazonaws.services.s3.model.PresignedUrlDownloadResult;
import com.amazonaws.services.s3.model.PresignedUrlUploadRequest;
import com.amazonaws.services.s3.model.PresignedUrlUploadResult;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.RestoreObjectRequest;
import com.amazonaws.services.s3.model.RestoreObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.SelectObjectContentRequest;
import com.amazonaws.services.s3.model.SelectObjectContentResult;
import com.amazonaws.services.s3.model.SetBucketAccelerateConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketAclRequest;
import com.amazonaws.services.s3.model.SetBucketAnalyticsConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketAnalyticsConfigurationResult;
import com.amazonaws.services.s3.model.SetBucketCrossOriginConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketEncryptionRequest;
import com.amazonaws.services.s3.model.SetBucketEncryptionResult;
import com.amazonaws.services.s3.model.SetBucketIntelligentTieringConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketIntelligentTieringConfigurationResult;
import com.amazonaws.services.s3.model.SetBucketInventoryConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketInventoryConfigurationResult;
import com.amazonaws.services.s3.model.SetBucketLifecycleConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketLoggingConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketMetricsConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketMetricsConfigurationResult;
import com.amazonaws.services.s3.model.SetBucketNotificationConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketOwnershipControlsRequest;
import com.amazonaws.services.s3.model.SetBucketOwnershipControlsResult;
import com.amazonaws.services.s3.model.SetBucketPolicyRequest;
import com.amazonaws.services.s3.model.SetBucketReplicationConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketTaggingConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketVersioningConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketWebsiteConfigurationRequest;
import com.amazonaws.services.s3.model.SetObjectAclRequest;
import com.amazonaws.services.s3.model.SetObjectLegalHoldRequest;
import com.amazonaws.services.s3.model.SetObjectLegalHoldResult;
import com.amazonaws.services.s3.model.SetObjectLockConfigurationRequest;
import com.amazonaws.services.s3.model.SetObjectLockConfigurationResult;
import com.amazonaws.services.s3.model.SetObjectRetentionRequest;
import com.amazonaws.services.s3.model.SetObjectRetentionResult;
import com.amazonaws.services.s3.model.SetObjectTaggingRequest;
import com.amazonaws.services.s3.model.SetObjectTaggingResult;
import com.amazonaws.services.s3.model.SetPublicAccessBlockRequest;
import com.amazonaws.services.s3.model.SetPublicAccessBlockResult;
import com.amazonaws.services.s3.model.SetRequestPaymentConfigurationRequest;
import com.amazonaws.services.s3.model.StorageClass;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.amazonaws.services.s3.model.VersionListing;
import com.amazonaws.services.s3.model.WriteGetObjectResponseRequest;
import com.amazonaws.services.s3.model.WriteGetObjectResponseResult;
import com.amazonaws.services.s3.model.analytics.AnalyticsConfiguration;
import com.amazonaws.services.s3.model.intelligenttiering.IntelligentTieringConfiguration;
import com.amazonaws.services.s3.model.inventory.InventoryConfiguration;
import com.amazonaws.services.s3.model.metrics.MetricsConfiguration;
import com.amazonaws.services.s3.model.ownership.OwnershipControls;
import com.amazonaws.services.s3.waiters.AmazonS3Waiters;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.List;

public abstract class AbstractFakeAmazonS3 implements AmazonS3 {

	@Override
	public void setEndpoint(String endpoint) {

	}

	@Override
	public void setS3ClientOptions(S3ClientOptions clientOptions) {

	}

	@Override
	public void changeObjectStorageClass(String bucketName, String key, StorageClass newStorageClass) throws SdkClientException, AmazonServiceException {

	}

	@Override
	public void setObjectRedirectLocation(String bucketName, String key, String newRedirectLocation) throws SdkClientException, AmazonServiceException {

	}

	@Override
	public ObjectListing listObjects(String bucketName) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public ObjectListing listObjects(String bucketName, String prefix) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public ObjectListing listObjects(ListObjectsRequest listObjectsRequest) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public ListObjectsV2Result listObjectsV2(String bucketName) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public ListObjectsV2Result listObjectsV2(String bucketName, String prefix) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public ListObjectsV2Result listObjectsV2(ListObjectsV2Request listObjectsV2Request) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public ObjectListing listNextBatchOfObjects(ObjectListing previousObjectListing) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public ObjectListing listNextBatchOfObjects(ListNextBatchOfObjectsRequest listNextBatchOfObjectsRequest) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public VersionListing listVersions(String bucketName, String prefix) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public VersionListing listNextBatchOfVersions(VersionListing previousVersionListing) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public VersionListing listNextBatchOfVersions(ListNextBatchOfVersionsRequest listNextBatchOfVersionsRequest) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public VersionListing listVersions(String bucketName, String prefix, String keyMarker, String versionIdMarker, String delimiter, Integer maxResults) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public VersionListing listVersions(ListVersionsRequest listVersionsRequest) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public Owner getS3AccountOwner() throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public Owner getS3AccountOwner(GetS3AccountOwnerRequest getS3AccountOwnerRequest) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public boolean doesBucketExist(String bucketName) throws SdkClientException, AmazonServiceException {
		return false;
	}

	@Override
	public boolean doesBucketExistV2(String bucketName) throws SdkClientException, AmazonServiceException {
		return false;
	}

	@Override
	public HeadBucketResult headBucket(HeadBucketRequest headBucketRequest) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public List<Bucket> listBuckets() throws SdkClientException, AmazonServiceException {
		return List.of();
	}

	@Override
	public List<Bucket> listBuckets(ListBucketsRequest listBucketsRequest) throws SdkClientException, AmazonServiceException {
		return List.of();
	}

	@Override
	public String getBucketLocation(String bucketName) throws SdkClientException, AmazonServiceException {
		return "";
	}

	@Override
	public String getBucketLocation(GetBucketLocationRequest getBucketLocationRequest) throws SdkClientException, AmazonServiceException {
		return "";
	}

	@Override
	public Bucket createBucket(CreateBucketRequest createBucketRequest) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public Bucket createBucket(String bucketName) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public Bucket createBucket(String bucketName, com.amazonaws.services.s3.model.Region region) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public Bucket createBucket(String bucketName, String region) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public AccessControlList getObjectAcl(String bucketName, String key) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public AccessControlList getObjectAcl(String bucketName, String key, String versionId) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public AccessControlList getObjectAcl(GetObjectAclRequest getObjectAclRequest) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public void setObjectAcl(String bucketName, String key, AccessControlList acl) throws SdkClientException, AmazonServiceException {

	}

	@Override
	public void setObjectAcl(String bucketName, String key, CannedAccessControlList acl) throws SdkClientException, AmazonServiceException {

	}

	@Override
	public void setObjectAcl(String bucketName, String key, String versionId, AccessControlList acl) throws SdkClientException, AmazonServiceException {

	}

	@Override
	public void setObjectAcl(String bucketName, String key, String versionId, CannedAccessControlList acl) throws SdkClientException, AmazonServiceException {

	}

	@Override
	public void setObjectAcl(SetObjectAclRequest setObjectAclRequest) throws SdkClientException, AmazonServiceException {

	}

	@Override
	public AccessControlList getBucketAcl(String bucketName) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public void setBucketAcl(SetBucketAclRequest setBucketAclRequest) throws SdkClientException, AmazonServiceException {

	}

	@Override
	public AccessControlList getBucketAcl(GetBucketAclRequest getBucketAclRequest) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public void setBucketAcl(String bucketName, AccessControlList acl) throws SdkClientException, AmazonServiceException {

	}

	@Override
	public void setBucketAcl(String bucketName, CannedAccessControlList acl) throws SdkClientException, AmazonServiceException {

	}

	@Override
	public ObjectMetadata getObjectMetadata(String bucketName, String key) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public ObjectMetadata getObjectMetadata(GetObjectMetadataRequest getObjectMetadataRequest) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public S3Object getObject(String bucketName, String key) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public S3Object getObject(GetObjectRequest getObjectRequest) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public ObjectMetadata getObject(GetObjectRequest getObjectRequest, File destinationFile) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public String getObjectAsString(String bucketName, String key) throws AmazonServiceException, SdkClientException {
		return "";
	}

	@Override
	public GetObjectTaggingResult getObjectTagging(GetObjectTaggingRequest getObjectTaggingRequest) {
		return null;
	}

	@Override
	public SetObjectTaggingResult setObjectTagging(SetObjectTaggingRequest setObjectTaggingRequest) {
		return null;
	}

	@Override
	public DeleteObjectTaggingResult deleteObjectTagging(DeleteObjectTaggingRequest deleteObjectTaggingRequest) {
		return null;
	}

	@Override
	public void deleteBucket(DeleteBucketRequest deleteBucketRequest) throws SdkClientException, AmazonServiceException {

	}

	@Override
	public void deleteBucket(String bucketName) throws SdkClientException, AmazonServiceException {

	}

	@Override
	public PutObjectResult putObject(PutObjectRequest putObjectRequest) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public PutObjectResult putObject(String bucketName, String key, File file) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public PutObjectResult putObject(String bucketName, String key, InputStream input, ObjectMetadata metadata) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public PutObjectResult putObject(String bucketName, String key, String content) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public CopyObjectResult copyObject(String sourceBucketName, String sourceKey, String destinationBucketName, String destinationKey) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public CopyObjectResult copyObject(CopyObjectRequest copyObjectRequest) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public CopyPartResult copyPart(CopyPartRequest copyPartRequest) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public void deleteObject(String bucketName, String key) throws SdkClientException, AmazonServiceException {

	}

	@Override
	public void deleteObject(DeleteObjectRequest deleteObjectRequest) throws SdkClientException, AmazonServiceException {

	}

	@Override
	public DeleteObjectsResult deleteObjects(DeleteObjectsRequest deleteObjectsRequest) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public void deleteVersion(String bucketName, String key, String versionId) throws SdkClientException, AmazonServiceException {

	}

	@Override
	public void deleteVersion(DeleteVersionRequest deleteVersionRequest) throws SdkClientException, AmazonServiceException {

	}

	@Override
	public BucketLoggingConfiguration getBucketLoggingConfiguration(String bucketName) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public BucketLoggingConfiguration getBucketLoggingConfiguration(GetBucketLoggingConfigurationRequest getBucketLoggingConfigurationRequest) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public void setBucketLoggingConfiguration(SetBucketLoggingConfigurationRequest setBucketLoggingConfigurationRequest) throws SdkClientException, AmazonServiceException {

	}

	@Override
	public BucketVersioningConfiguration getBucketVersioningConfiguration(String bucketName) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public BucketVersioningConfiguration getBucketVersioningConfiguration(GetBucketVersioningConfigurationRequest getBucketVersioningConfigurationRequest) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public void setBucketVersioningConfiguration(SetBucketVersioningConfigurationRequest setBucketVersioningConfigurationRequest) throws SdkClientException, AmazonServiceException {

	}

	@Override
	public BucketLifecycleConfiguration getBucketLifecycleConfiguration(String bucketName) {
		return null;
	}

	@Override
	public BucketLifecycleConfiguration getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest getBucketLifecycleConfigurationRequest) {
		return null;
	}

	@Override
	public void setBucketLifecycleConfiguration(String bucketName, BucketLifecycleConfiguration bucketLifecycleConfiguration) {

	}

	@Override
	public void setBucketLifecycleConfiguration(SetBucketLifecycleConfigurationRequest setBucketLifecycleConfigurationRequest) {

	}

	@Override
	public void deleteBucketLifecycleConfiguration(String bucketName) {

	}

	@Override
	public void deleteBucketLifecycleConfiguration(DeleteBucketLifecycleConfigurationRequest deleteBucketLifecycleConfigurationRequest) {

	}

	@Override
	public BucketCrossOriginConfiguration getBucketCrossOriginConfiguration(String bucketName) {
		return null;
	}

	@Override
	public BucketCrossOriginConfiguration getBucketCrossOriginConfiguration(GetBucketCrossOriginConfigurationRequest getBucketCrossOriginConfigurationRequest) {
		return null;
	}

	@Override
	public void setBucketCrossOriginConfiguration(String bucketName, BucketCrossOriginConfiguration bucketCrossOriginConfiguration) {

	}

	@Override
	public void setBucketCrossOriginConfiguration(SetBucketCrossOriginConfigurationRequest setBucketCrossOriginConfigurationRequest) {

	}

	@Override
	public void deleteBucketCrossOriginConfiguration(String bucketName) {

	}

	@Override
	public void deleteBucketCrossOriginConfiguration(DeleteBucketCrossOriginConfigurationRequest deleteBucketCrossOriginConfigurationRequest) {

	}

	@Override
	public BucketTaggingConfiguration getBucketTaggingConfiguration(String bucketName) {
		return null;
	}

	@Override
	public BucketTaggingConfiguration getBucketTaggingConfiguration(GetBucketTaggingConfigurationRequest getBucketTaggingConfigurationRequest) {
		return null;
	}

	@Override
	public void setBucketTaggingConfiguration(String bucketName, BucketTaggingConfiguration bucketTaggingConfiguration) {

	}

	@Override
	public void setBucketTaggingConfiguration(SetBucketTaggingConfigurationRequest setBucketTaggingConfigurationRequest) {

	}

	@Override
	public void deleteBucketTaggingConfiguration(String bucketName) {

	}

	@Override
	public void deleteBucketTaggingConfiguration(DeleteBucketTaggingConfigurationRequest deleteBucketTaggingConfigurationRequest) {

	}

	@Override
	public BucketNotificationConfiguration getBucketNotificationConfiguration(String bucketName) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public BucketNotificationConfiguration getBucketNotificationConfiguration(GetBucketNotificationConfigurationRequest getBucketNotificationConfigurationRequest) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public void setBucketNotificationConfiguration(SetBucketNotificationConfigurationRequest setBucketNotificationConfigurationRequest) throws SdkClientException, AmazonServiceException {

	}

	@Override
	public void setBucketNotificationConfiguration(String bucketName, BucketNotificationConfiguration bucketNotificationConfiguration) throws SdkClientException, AmazonServiceException {

	}

	@Override
	public BucketWebsiteConfiguration getBucketWebsiteConfiguration(String bucketName) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public BucketWebsiteConfiguration getBucketWebsiteConfiguration(GetBucketWebsiteConfigurationRequest getBucketWebsiteConfigurationRequest) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public void setBucketWebsiteConfiguration(String bucketName, BucketWebsiteConfiguration configuration) throws SdkClientException, AmazonServiceException {

	}

	@Override
	public void setBucketWebsiteConfiguration(SetBucketWebsiteConfigurationRequest setBucketWebsiteConfigurationRequest) throws SdkClientException, AmazonServiceException {

	}

	@Override
	public void deleteBucketWebsiteConfiguration(String bucketName) throws SdkClientException, AmazonServiceException {

	}

	@Override
	public void deleteBucketWebsiteConfiguration(DeleteBucketWebsiteConfigurationRequest deleteBucketWebsiteConfigurationRequest) throws SdkClientException, AmazonServiceException {

	}

	@Override
	public BucketPolicy getBucketPolicy(String bucketName) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public BucketPolicy getBucketPolicy(GetBucketPolicyRequest getBucketPolicyRequest) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public void setBucketPolicy(String bucketName, String policyText) throws SdkClientException, AmazonServiceException {

	}

	@Override
	public void setBucketPolicy(SetBucketPolicyRequest setBucketPolicyRequest) throws SdkClientException, AmazonServiceException {

	}

	@Override
	public void deleteBucketPolicy(String bucketName) throws SdkClientException, AmazonServiceException {

	}

	@Override
	public void deleteBucketPolicy(DeleteBucketPolicyRequest deleteBucketPolicyRequest) throws SdkClientException, AmazonServiceException {

	}

	@Override
	public URL generatePresignedUrl(String bucketName, String key, Date expiration) throws SdkClientException {
		return null;
	}

	@Override
	public URL generatePresignedUrl(String bucketName, String key, Date expiration, HttpMethod method) throws SdkClientException {
		return null;
	}

	@Override
	public URL generatePresignedUrl(GeneratePresignedUrlRequest generatePresignedUrlRequest) throws SdkClientException {
		return null;
	}

	@Override
	public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public UploadPartResult uploadPart(UploadPartRequest request) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public PartListing listParts(ListPartsRequest request) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public void abortMultipartUpload(AbortMultipartUploadRequest request) throws SdkClientException, AmazonServiceException {

	}

	@Override
	public CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest request) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public MultipartUploadListing listMultipartUploads(ListMultipartUploadsRequest request) throws SdkClientException, AmazonServiceException {
		return null;
	}

	@Override
	public S3ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest request) {
		return null;
	}

	@Override
	public void restoreObject(RestoreObjectRequest request) throws AmazonServiceException {

	}

	@Override
	public RestoreObjectResult restoreObjectV2(RestoreObjectRequest request) throws AmazonServiceException {
		return null;
	}

	@Override
	public void restoreObject(String bucketName, String key, int expirationInDays) throws AmazonServiceException {

	}

	@Override
	public void enableRequesterPays(String bucketName) throws AmazonServiceException, SdkClientException {

	}

	@Override
	public void disableRequesterPays(String bucketName) throws AmazonServiceException, SdkClientException {

	}

	@Override
	public boolean isRequesterPaysEnabled(String bucketName) throws AmazonServiceException, SdkClientException {
		return false;
	}

	@Override
	public void setRequestPaymentConfiguration(SetRequestPaymentConfigurationRequest setRequestPaymentConfigurationRequest) {

	}

	@Override
	public void setBucketReplicationConfiguration(String bucketName, BucketReplicationConfiguration configuration) throws AmazonServiceException, SdkClientException {

	}

	@Override
	public void setBucketReplicationConfiguration(SetBucketReplicationConfigurationRequest setBucketReplicationConfigurationRequest) throws AmazonServiceException, SdkClientException {

	}

	@Override
	public BucketReplicationConfiguration getBucketReplicationConfiguration(String bucketName) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public BucketReplicationConfiguration getBucketReplicationConfiguration(GetBucketReplicationConfigurationRequest getBucketReplicationConfigurationRequest) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public void deleteBucketReplicationConfiguration(String bucketName) throws AmazonServiceException, SdkClientException {

	}

	@Override
	public void deleteBucketReplicationConfiguration(DeleteBucketReplicationConfigurationRequest request) throws AmazonServiceException, SdkClientException {

	}

	@Override
	public boolean doesObjectExist(String bucketName, String objectName) throws AmazonServiceException, SdkClientException {
		return false;
	}

	@Override
	public BucketAccelerateConfiguration getBucketAccelerateConfiguration(String bucketName) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public BucketAccelerateConfiguration getBucketAccelerateConfiguration(GetBucketAccelerateConfigurationRequest getBucketAccelerateConfigurationRequest) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public void setBucketAccelerateConfiguration(String bucketName, BucketAccelerateConfiguration accelerateConfiguration) throws AmazonServiceException, SdkClientException {

	}

	@Override
	public void setBucketAccelerateConfiguration(SetBucketAccelerateConfigurationRequest setBucketAccelerateConfigurationRequest) throws AmazonServiceException, SdkClientException {

	}

	@Override
	public DeleteBucketMetricsConfigurationResult deleteBucketMetricsConfiguration(String bucketName, String id) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public DeleteBucketMetricsConfigurationResult deleteBucketMetricsConfiguration(DeleteBucketMetricsConfigurationRequest deleteBucketMetricsConfigurationRequest) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public GetBucketMetricsConfigurationResult getBucketMetricsConfiguration(String bucketName, String id) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public GetBucketMetricsConfigurationResult getBucketMetricsConfiguration(GetBucketMetricsConfigurationRequest getBucketMetricsConfigurationRequest) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public SetBucketMetricsConfigurationResult setBucketMetricsConfiguration(String bucketName, MetricsConfiguration metricsConfiguration) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public SetBucketMetricsConfigurationResult setBucketMetricsConfiguration(SetBucketMetricsConfigurationRequest setBucketMetricsConfigurationRequest) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public ListBucketMetricsConfigurationsResult listBucketMetricsConfigurations(ListBucketMetricsConfigurationsRequest listBucketMetricsConfigurationsRequest) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public DeleteBucketOwnershipControlsResult deleteBucketOwnershipControls(DeleteBucketOwnershipControlsRequest deleteBucketOwnershipControlsRequest) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public GetBucketOwnershipControlsResult getBucketOwnershipControls(GetBucketOwnershipControlsRequest getBucketOwnershipControlsRequest) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public SetBucketOwnershipControlsResult setBucketOwnershipControls(String bucketName, OwnershipControls ownershipControls) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public SetBucketOwnershipControlsResult setBucketOwnershipControls(SetBucketOwnershipControlsRequest setBucketOwnershipControlsRequest) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public DeleteBucketAnalyticsConfigurationResult deleteBucketAnalyticsConfiguration(String bucketName, String id) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public DeleteBucketAnalyticsConfigurationResult deleteBucketAnalyticsConfiguration(DeleteBucketAnalyticsConfigurationRequest deleteBucketAnalyticsConfigurationRequest) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public GetBucketAnalyticsConfigurationResult getBucketAnalyticsConfiguration(String bucketName, String id) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public GetBucketAnalyticsConfigurationResult getBucketAnalyticsConfiguration(GetBucketAnalyticsConfigurationRequest getBucketAnalyticsConfigurationRequest) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public SetBucketAnalyticsConfigurationResult setBucketAnalyticsConfiguration(String bucketName, AnalyticsConfiguration analyticsConfiguration) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public SetBucketAnalyticsConfigurationResult setBucketAnalyticsConfiguration(SetBucketAnalyticsConfigurationRequest setBucketAnalyticsConfigurationRequest) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public ListBucketAnalyticsConfigurationsResult listBucketAnalyticsConfigurations(ListBucketAnalyticsConfigurationsRequest listBucketAnalyticsConfigurationsRequest) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public DeleteBucketIntelligentTieringConfigurationResult deleteBucketIntelligentTieringConfiguration(String bucketName, String id) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public DeleteBucketIntelligentTieringConfigurationResult deleteBucketIntelligentTieringConfiguration(DeleteBucketIntelligentTieringConfigurationRequest deleteBucketIntelligentTieringConfigurationRequest) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public GetBucketIntelligentTieringConfigurationResult getBucketIntelligentTieringConfiguration(String bucketName, String id) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public GetBucketIntelligentTieringConfigurationResult getBucketIntelligentTieringConfiguration(GetBucketIntelligentTieringConfigurationRequest getBucketIntelligentTieringConfigurationRequest) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public SetBucketIntelligentTieringConfigurationResult setBucketIntelligentTieringConfiguration(String bucketName, IntelligentTieringConfiguration intelligentTieringConfiguration) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public SetBucketIntelligentTieringConfigurationResult setBucketIntelligentTieringConfiguration(SetBucketIntelligentTieringConfigurationRequest setBucketIntelligentTieringConfigurationRequest) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public ListBucketIntelligentTieringConfigurationsResult listBucketIntelligentTieringConfigurations(ListBucketIntelligentTieringConfigurationsRequest listBucketIntelligentTieringConfigurationsRequest) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public DeleteBucketInventoryConfigurationResult deleteBucketInventoryConfiguration(String bucketName, String id) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public DeleteBucketInventoryConfigurationResult deleteBucketInventoryConfiguration(DeleteBucketInventoryConfigurationRequest deleteBucketInventoryConfigurationRequest) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public GetBucketInventoryConfigurationResult getBucketInventoryConfiguration(String bucketName, String id) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public GetBucketInventoryConfigurationResult getBucketInventoryConfiguration(GetBucketInventoryConfigurationRequest getBucketInventoryConfigurationRequest) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public SetBucketInventoryConfigurationResult setBucketInventoryConfiguration(String bucketName, InventoryConfiguration inventoryConfiguration) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public SetBucketInventoryConfigurationResult setBucketInventoryConfiguration(SetBucketInventoryConfigurationRequest setBucketInventoryConfigurationRequest) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public ListBucketInventoryConfigurationsResult listBucketInventoryConfigurations(ListBucketInventoryConfigurationsRequest listBucketInventoryConfigurationsRequest) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public DeleteBucketEncryptionResult deleteBucketEncryption(String bucketName) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public DeleteBucketEncryptionResult deleteBucketEncryption(DeleteBucketEncryptionRequest request) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public GetBucketEncryptionResult getBucketEncryption(String bucketName) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public GetBucketEncryptionResult getBucketEncryption(GetBucketEncryptionRequest request) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public SetBucketEncryptionResult setBucketEncryption(SetBucketEncryptionRequest setBucketEncryptionRequest) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public SetPublicAccessBlockResult setPublicAccessBlock(SetPublicAccessBlockRequest request) {
		return null;
	}

	@Override
	public GetPublicAccessBlockResult getPublicAccessBlock(GetPublicAccessBlockRequest request) {
		return null;
	}

	@Override
	public DeletePublicAccessBlockResult deletePublicAccessBlock(DeletePublicAccessBlockRequest request) {
		return null;
	}

	@Override
	public GetBucketPolicyStatusResult getBucketPolicyStatus(GetBucketPolicyStatusRequest request) {
		return null;
	}

	@Override
	public SelectObjectContentResult selectObjectContent(SelectObjectContentRequest selectRequest) throws AmazonServiceException, SdkClientException {
		return null;
	}

	@Override
	public SetObjectLegalHoldResult setObjectLegalHold(SetObjectLegalHoldRequest setObjectLegalHoldRequest) {
		return null;
	}

	@Override
	public GetObjectLegalHoldResult getObjectLegalHold(GetObjectLegalHoldRequest getObjectLegalHoldRequest) {
		return null;
	}

	@Override
	public SetObjectLockConfigurationResult setObjectLockConfiguration(SetObjectLockConfigurationRequest setObjectLockConfigurationRequest) {
		return null;
	}

	@Override
	public GetObjectLockConfigurationResult getObjectLockConfiguration(GetObjectLockConfigurationRequest getObjectLockConfigurationRequest) {
		return null;
	}

	@Override
	public SetObjectRetentionResult setObjectRetention(SetObjectRetentionRequest setObjectRetentionRequest) {
		return null;
	}

	@Override
	public GetObjectRetentionResult getObjectRetention(GetObjectRetentionRequest getObjectRetentionRequest) {
		return null;
	}

	@Override
	public WriteGetObjectResponseResult writeGetObjectResponse(WriteGetObjectResponseRequest writeGetObjectResponseRequest) {
		return null;
	}

	@Override
	public PresignedUrlDownloadResult download(PresignedUrlDownloadRequest presignedUrlDownloadRequest) {
		return null;
	}

	@Override
	public void download(PresignedUrlDownloadRequest presignedUrlDownloadRequest, File destinationFile) {

	}

	@Override
	public PresignedUrlUploadResult upload(PresignedUrlUploadRequest presignedUrlUploadRequest) {
		return null;
	}

	@Override
	public void shutdown() {

	}

	@Override
	public com.amazonaws.services.s3.model.Region getRegion() {
		return null;
	}

	@Override
	public void setRegion(Region region) throws IllegalArgumentException {

	}

	@Override
	public String getRegionName() {
		return "";
	}

	@Override
	public URL getUrl(String bucketName, String key) {
		return null;
	}

	@Override
	public AmazonS3Waiters waiters() {
		return null;
	}
}
