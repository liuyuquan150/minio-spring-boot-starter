package indi.ly.crush.autoconfigure;

import io.minio.MinioAsyncClient;
import io.minio.MinioClient;
import io.minio.credentials.ChainedProvider;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * <h2>MinIo 自动配置</h2>
 * <p>
 *     MinIo 的自动配置, 以下的自动配置将被应用:
 *     <ol>
 *         <li>一个 {@link MinioClient MinioClient Bean}, 如果还没有配置的话.</li>
 *         <li>一个 {@link MinioAsyncClient MinioAsyncClient Bean}, 如果还没有配置的话.</li>
 *     </ol>
 *
 *     windows minio 服务启动命令: minio.exe server /data
 * </p>
 *
 * @author 云上的云
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MinIoProperties.class)
public class MinIoAutoConfiguration {
	
	@Bean
	@ConditionalOnMissingBean
	public MinioClient createMinioClientBean(ApplicationContext context, MinIoProperties properties) {
		MinioClient.Builder clientBuilder = MinioClient
												.builder()
												.credentialsProvider(new ChainedProvider())
												.endpoint(properties.getEndpoint())
												.region(properties.getRegion())
												.credentials(properties.getAccessKey(), properties.getSecretKey());
		
		String okHttpClientBeanName = properties.getOkHttpClientBeanName();
		if (StringUtils.hasText(okHttpClientBeanName)) {
			try {
				OkHttpClient okHttpClient = context.getBean(okHttpClientBeanName, OkHttpClient.class);
				clientBuilder.httpClient(okHttpClient);
			} catch (Exception ignore) {}
		}
		
		MinioClient client = clientBuilder.build();
		client.setTimeout(
				properties.getConnectTimeout().toMillis(),
				properties.getWriteTimeout().toMillis(),
				properties.getReadTimeout().toMillis()
		);
		
		return client;
	}
	
	@Bean
	@ConditionalOnMissingBean
	public MinioAsyncClient createMinioAsyncClientBean(ApplicationContext context, MinIoProperties properties) {
		MinioAsyncClient.Builder asyncClientBuilder = MinioAsyncClient
															.builder()
															.endpoint(properties.getEndpoint())
															.region(properties.getRegion())
															.credentials(properties.getAccessKey(), properties.getSecretKey());
		
		String okHttpClientBeanName = properties.getOkHttpClientBeanName();
		if (StringUtils.hasText(okHttpClientBeanName)) {
			try {
				OkHttpClient okHttpClient = context.getBean(okHttpClientBeanName, OkHttpClient.class);
				asyncClientBuilder.httpClient(okHttpClient);
			} catch (Exception ignore) {}
		}
		
		MinioAsyncClient asyncClient = asyncClientBuilder.build();
		asyncClient.setTimeout(
				properties.getConnectTimeout().toMillis(),
				properties.getWriteTimeout().toMillis(),
				properties.getReadTimeout().toMillis()
		);
		
		return asyncClient;
	}
}
