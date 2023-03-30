package indi.ly.crush.helper;

import indi.ly.crush.autoconfigure.MinIoProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioAsyncClient;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import io.minio.messages.Bucket;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.StreamUtils;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * <h2>MinIo 桶操作</h2>
 *
 * @author 云上的云
 * @since 1.0
 */
class MinIoBucketOperation
		implements ResourceLoaderAware {
	protected final MinioClient minioClient;
	protected final MinioAsyncClient minioAsyncClient;
	protected final String endpoint;
	protected final String region;
	protected final String bucketName;
	private final Boolean bucketPolicy;
	private final String policyLocation;
	protected final Log logger = LogFactory.getLog(this.getClass());
	/**
	 * <p>
	 *     一段 JSON 字符串, 可以设置桶的访问策略为 Public(<em>可以直接访问到文件, 安全性最低</em>),
	 *     一般场景用于各种针对于用户不需要登录就能访问的照片、文件等. <br /> <br />
	 *
	 *
	 *     请注意, JSON 字符串中的 %s 最终会替换为 {@link #bucketName}.
	 * </p>
	 */
	private static final String BUCKET_PUBLIC_POLICY =
        """
			{
				"Version": "2012-10-17",
				"Statement": [
					{
						"Effect": "Allow",
						"Principal": {
							"AWS": ["*"]
						},
						"Action": ["s3:ListBucket", "s3:ListBucketMultipartUploads", "s3:GetBucketLocation"],
						"Resource": ["arn:aws:s3:::%s"]
					},
					{
						"Effect": "Allow",
						"Principal": {
							"AWS": ["*"]
						},
						"Action": [
							"s3:AbortMultipartUpload", "s3:DeleteObject",
							"s3:GetObject", "s3:ListMultipartUploadParts", "s3:PutObject"
						],
						"Resource": ["arn:aws:s3:::%s/*"]
					}
				]
			}
		""";
	private static final String JSON_SUFFIX = ".json";
	private ResourcePatternResolver resourcePatternResolver;
	
	public MinIoBucketOperation(MinioClient minioClient, MinioAsyncClient minioAsyncClient, MinIoProperties properties) {
		this.minioClient = Objects.requireNonNull(minioClient, "minioClient is null");
		this.minioAsyncClient = Objects.requireNonNull(minioAsyncClient, "minioAsyncClient is null");
		this.endpoint = Objects.requireNonNull(properties.getEndpoint(), "endpoint is null");
		this.bucketName = Objects.requireNonNull(properties, "properties is null").getBucketName();
		this.region = properties.getRegion();
		this.bucketPolicy = properties.getBucketPolicy();
		
		String policyLocation = properties.getPolicyLocation();
		if (policyLocation == null && this.bucketPolicy == null) {
			throw new IllegalArgumentException("'bucketPolicy' and 'policyLocation' always have to provide a");
		}
		if (policyLocation != null && !policyLocation.endsWith(JSON_SUFFIX)) {
			policyLocation += JSON_SUFFIX;
			properties.setPolicyLocation(policyLocation);
		}
		this.policyLocation = policyLocation;
		
		if (this.logger.isDebugEnabled()) {
			this.logger.debug(properties);
		}
	}
	
	protected List<Bucket> listBuckets(){
		try {
			return this.minioClient.listBuckets();
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	private Boolean bucketExists() {
		BucketExistsArgs args = BucketExistsArgs
											.builder()
											.bucket(this.bucketName)
											.region(this.region)
											.build();
		try {
			return this.minioClient.bucketExists(args);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	private void makeBucket(){
		if (this.bucketExists()) {
			this.logger.info("The MinIo bucket with the name '" + this.bucketName + "' already exists and does not need to be created");
			return;
		}
		
		MakeBucketArgs args = MakeBucketArgs
										.builder()
										.bucket(this.bucketName)
										.region(this.region)
										.build();
		String permission = "custom";
		try {
			// 创建好的桶的访问权限默认是 private.
			this.minioClient.makeBucket(args);
			
			if (this.policyLocation != null) {
				Resource resource = this.resourcePatternResolver.getResource(this.policyLocation);
				String customPolicyJson = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
				this.setBucketPolicy(customPolicyJson);
			} else {
				if (this.bucketPolicy) {
					String publicPolicyJson = BUCKET_PUBLIC_POLICY.formatted(this.bucketName, this.bucketName);
					this.setBucketPolicy(publicPolicyJson);
				}
				permission = (this.bucketPolicy ? "public" : "private");
			}
			
			String finalPermission = permission;
			LogFormatUtils.traceDebug(this.logger,
					traceOn -> LogFormatUtils.formatValue(
							"MinIo bucket with name '%s' created successfully, There is a '%s' permission".formatted(
									this.bucketName, finalPermission), !traceOn));
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	private void setBucketPolicy(String policyJson) {
		SetBucketPolicyArgs args = SetBucketPolicyArgs
												.builder()
												.bucket(this.bucketName)
												.region(this.region)
												.config(policyJson)
												.build();
		try {
			this.minioClient.setBucketPolicy(args);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	@PostConstruct
	private void doMakeBucket() {
		this.makeBucket();
		List<Bucket> buckets = this.listBuckets();
		Supplier<String> messageSupplier = () -> buckets
													.stream()
													.map(bucket -> bucket.creationDate().plusHours(8) + "-" + bucket.name())
													.collect(Collectors.joining(", ", "[", "]"));
		LogFormatUtils.traceDebug(this.logger,
				traceOn -> "MinIo All Bucket(Format: Creation time - Name): %s".formatted(
						LogFormatUtils.formatValue(buckets.isEmpty() ? "[]" : messageSupplier.get(), !traceOn)));
	}
	
	@Override
	public void setResourceLoader(@NonNull ResourceLoader resourceLoader) {
		this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
	}
}
