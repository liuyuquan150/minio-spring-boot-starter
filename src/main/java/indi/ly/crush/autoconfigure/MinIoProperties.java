package indi.ly.crush.autoconfigure;

import io.minio.MinioAsyncClient;
import io.minio.http.HttpUtils;
import okhttp3.OkHttpClient;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * <h2>MinIo 属性</h2>
 * <p>
 *     用于配置 MinIO.
 * </p>
 *
 * @author 云上的云
 * @since 1.0
 */
@ConfigurationProperties(prefix = "customize.minio")
public class MinIoProperties {
	/**
	 * <p>
	 *     MinIO 服务器的访问地址, 默认设置为内网下的访问地址(<em>即 http://localhost:9000</em>).
	 * </p>
	 */
	private String endpoint = "http://localhost:9000";
	/**
	 * <p>
	 *     MinIO 服务器所在的物理地址, 可以为 null.
	 * </p>
	 */
	private String region;
	/**
	 * <p>
	 *     Root 用户的访问密钥(<em>通信的用户名</em>), 默认值为 minioadmin.
	 * </p>
	 */
	private String accessKey = "minioadmin";
	/**
	 * <p>
	 *     Root 用户的密钥(<em>通信的密码</em>), 默认值为 minioadmin.
	 * </p>
	 */
	private String secretKey = "minioadmin";
	/**
	 * <p>
	 *     HTTP 的连接超时(<em>毫秒, 默认为 10 毫秒</em>), 值必须在 1 和 {@link Integer#MAX_VALUE} 之间, 值为 0 意味着没有超时.
	 * </p>
	 */
	private Duration connectTimeout = Duration.ofSeconds(10);
	/**
	 * <p>
	 *     HTTP 的写入超时(<em>毫秒, 默认为 60 毫秒</em>), 值必须在 1 和 {@link Integer#MAX_VALUE} 之间, 值为 0 意味着没有超时.
	 * </p>
	 */
	private Duration writeTimeout = Duration.ofSeconds(60);
	/**
	 * <p>
	 *     HTTP 的读取超时(<em>毫秒, 默认为 10 毫秒</em>), 值必须在 1 和 {@link Integer#MAX_VALUE} 之间, 值为 0 意味着没有超时.
	 * </p>
	 */
	private Duration readTimeout = Duration.ofSeconds(10);
	/**
	 * <p>
	 *     桶名称(<em>禁止使用以 %s/%S 开头的名称, 原因在 MinIoBucketOperation#BUCKET_PUBLIC_POLICY 中</em>), 默认值为 default.
	 * </p>
	 */
	private String bucketName = "default";
	/**
	 * <p>
	 *     桶权限(<em>true: public; false: private</em>), 默认值为 true.
	 * </p>
	 */
	private Boolean bucketPolicy = true;
	/**
	 * <p>
	 *     这是一个 JSON 文件的资源位置, 用于自定义桶的权限.
	 *     此属性与 {@link #bucketPolicy} 互斥, 且优先级高于 {@link #bucketPolicy}.
	 * </p>
	 */
	private String policyLocation;
	/**
	 * <p>
	 *     {@link OkHttpClient} Bean 的名称. 如果根据此名称无法检索出匹配的 Bean, 则仍然使用默认的 {@link OkHttpClient} 实例.
	 * </p>
	 *
	 * @see MinioAsyncClient.Builder#build()
	 * @see HttpUtils#newDefaultHttpClient(long, long, long)
	 */
	private String okHttpClientBeanName;
	
	public String getEndpoint() {
		return endpoint;
	}
	
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}
	
	public String getRegion() {
		return region;
	}
	
	public void setRegion(String region) {
		this.region = region;
	}
	
	public String getAccessKey() {
		return accessKey;
	}
	
	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}
	
	public String getSecretKey() {
		return secretKey;
	}
	
	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}
	
	public Duration getConnectTimeout() {
		return connectTimeout;
	}
	
	public void setConnectTimeout(Duration connectTimeout) {
		this.connectTimeout = connectTimeout;
	}
	
	public Duration getWriteTimeout() {
		return writeTimeout;
	}
	
	public void setWriteTimeout(Duration writeTimeout) {
		this.writeTimeout = writeTimeout;
	}
	
	public Duration getReadTimeout() {
		return readTimeout;
	}
	
	public void setReadTimeout(Duration readTimeout) {
		this.readTimeout = readTimeout;
	}
	
	public String getBucketName() {
		return bucketName;
	}
	
	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}
	
	public Boolean getBucketPolicy() {
		return bucketPolicy;
	}
	
	public void setBucketPolicy(Boolean bucketPolicy) {
		this.bucketPolicy = bucketPolicy;
	}
	
	public String getPolicyLocation() {
		return policyLocation;
	}
	
	public void setPolicyLocation(String policyLocation) {
		this.policyLocation = policyLocation;
	}
	
	public String getOkHttpClientBeanName() {
		return okHttpClientBeanName;
	}
	
	public void setOkHttpClientBeanName(String okHttpClientBeanName) {
		this.okHttpClientBeanName = okHttpClientBeanName;
	}
	
	@Override
	public String toString() {
		return "MinIoProperties{" + "endpoint='" + endpoint + '\'' + ", region='" + region + '\'' + ", accessKey='"
				+ accessKey + '\'' + ", secretKey='" + secretKey + '\'' + ", connectTimeout=" + connectTimeout
				+ ", writeTimeout=" + writeTimeout + ", readTimeout=" + readTimeout + ", bucketName='" + bucketName
				+ '\'' + ", bucketPolicy=" + bucketPolicy + ", policyLocation='" + policyLocation + '\''
				+ ", okHttpClientBeanName='" + okHttpClientBeanName + '\'' + '}';
	}
}
