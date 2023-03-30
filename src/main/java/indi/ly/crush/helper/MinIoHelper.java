package indi.ly.crush.helper;

import indi.ly.crush.autoconfigure.MinIoProperties;
import indi.ly.crush.model.UploadResultResponse;
import io.minio.ListObjectsArgs;
import io.minio.MinioAsyncClient;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SnowballObject;
import io.minio.UploadSnowballObjectsArgs;
import io.minio.messages.Item;
import org.apache.tika.Tika;
import org.apache.tika.mime.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * <h2>MinIo 助手</h2>
 *
 * @author 云上的云
 * @since 1.0
 */
@Component
public class MinIoHelper
		extends MinIoBucketOperation {
	final String prefix;
	final Tika tika = new Tika();
	
	public MinIoHelper(MinioClient minioClient, MinioAsyncClient minioAsyncClient, MinIoProperties properties) {
		super(minioClient, minioAsyncClient, properties);
		this.prefix = super.endpoint + '/' + super.bucketName;
	}
	
	/**
	 * <p>
	 *     语义继承 {@link #uploadFile(InputStream, String, String)} 方法,
	 *     在此基础上省略了 contentType 参数的传入, 由方法内部根据 inputStream 中的内容来分析出 contentType(<em>内容类型</em>).
	 * </p>
	 *
	 * @param inputStream 上传文件的流形式.
	 * @param location    上传文件存放在桶中的位置, 如: "xxx/xxx/xxx/文件名.文件扩展名"、"文件名.文件扩展名".
	 * @return 一个上传结果响应实例.
	 */
	public UploadResultResponse uploadFile(InputStream inputStream, String location) {
		Assert.notNull(inputStream, "stream is null");
		Assert.state(StringUtils.hasLength(location), "location is not valid");
		
		String contentType;
		byte[] bytes;
		try {
			bytes = inputStream.readAllBytes();
			// 比 HttpURLConnection.guessContentTypeFromStream(stream); 可靠.
			contentType = this.tika.detect(bytes);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		
		if (MediaType.OCTET_STREAM.toString().equals(contentType)) {
			throw new RuntimeException("Unable to identify the content type of the stream");
		}
		
		return this.uploadFile(new ByteArrayInputStream(bytes), location, contentType);
	}
	
	/**
	 * <p>
	 *     单文件上传至指定 {@link #bucketName} 桶的指定位置, 该桶所在 {@link #region}.
	 * </p>
	 *
	 * @param inputStream 上传文件的流形式.
	 * @param location    上传文件存放在桶中的位置, 如: "xxx/xxx/xxx/文件名.文件扩展名"、"文件名.文件扩展名".
 	 * @param contentType 上传文件的内容类型.
	 * @return 一个上传结果响应实例.
	 */
	public UploadResultResponse uploadFile(InputStream inputStream, String location, String contentType) {
		Assert.notNull(inputStream, "inputStream is null");
		Assert.state(StringUtils.hasLength(location), "location is not valid");
		Assert.state(StringUtils.hasLength(contentType), "contentType is not valid");
		
		try (inputStream) {
			PutObjectArgs args = PutObjectArgs
											.builder()
											.bucket(super.bucketName)
											.region(super.region)
											.object(location)
											.stream(inputStream, inputStream.available(), -1)
											.contentType(contentType)
											.build();
			ObjectWriteResponse response = super.minioClient.putObject(args);
			String url = "%s/%s/%s".formatted(super.endpoint, super.bucketName, location);
			return new UploadResultResponse(url, response);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	/**
	 * <p>
	 *     删除指定 {@link #bucketName} 桶中指定位置的单文件.
	 * </p>
	 *
	 * @param fileAccessUrl MinIO 服务器提供给外界的一个完整 URL.
	 */
	public void removeFile(String fileAccessUrl) {
		Assert.state(StringUtils.hasLength(fileAccessUrl), "location is not valid");
		
		// 要删除的文件在 Bucket(桶) 中的位置.
		fileAccessUrl = fileAccessUrl.substring(this.prefix.length());
		
		List<Item> items = this.searchFile(fileAccessUrl);
		if (items.isEmpty()) {
			throw new IllegalArgumentException("This file does not exist on the 'MinIO' OSS server, and the access path is: " + this.prefix + fileAccessUrl);
		}
		if (items.size() > 1) {
			List<String> itemNames = items
										.stream()
										.map(Item :: objectName)
										.toList();
			throw new IllegalArgumentException("Multiple files with the same access path were found that can be deleted: " + itemNames);
		}
		
		RemoveObjectArgs args = RemoveObjectArgs
											.builder()
											.bucket(this.bucketName)
											.region(this.region)
											.object(fileAccessUrl)
											.build();
		try {
			this.minioClient.removeObject(args);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	public List<Item> searchFile(String prefix) {
		Assert.state(StringUtils.hasLength(prefix), "prefix is not valid");
		return this.searchFile(builder -> builder.prefix(prefix));
	}
	
	public List<Item> searchFile(Consumer<ListObjectsArgs.Builder> builderConsumer) {
		return this.search(builderConsumer, item -> !item.isDir());
	}
	
	public List<Item> search(Consumer<ListObjectsArgs.Builder> builderConsume) {
		return this.search(builderConsume, item -> true);
	}
	
	private List<Item> search(Consumer<ListObjectsArgs.Builder> builderConsumer, Predicate<Item> itemPredicate) {
		ListObjectsArgs.Builder builder = ListObjectsArgs
													.builder()
													.bucket(this.bucketName)
													.region(this.region);
		
		if (builderConsumer != null) {
			builderConsumer.accept(builder);
		}
		
		List<Item> items = new LinkedList<>();
		
		this.minioClient.listObjects(builder.build()).forEach(result -> {
			try {
				Item item = result.get();
				if (itemPredicate.test(item)) {
					items.add(item);
				}
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		});
		
		return items;
	}
	
	public void uploadFiles(Map<String, InputStream> fileMap) {
		List<SnowballObject> snowballObjectList = fileMap
														.entrySet()
														.stream()
														.map(MinIoHelper :: convertSnowballObject)
														.toList();
		UploadSnowballObjectsArgs args = UploadSnowballObjectsArgs
														.builder()
														.bucket(super.bucketName)
														.region(super.region)
														.objects(snowballObjectList)
														.build();
		try {
			ObjectWriteResponse response = super.minioClient.uploadSnowballObjects(args);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	static SnowballObject convertSnowballObject(Map.Entry<String, InputStream> entry) {
		try (InputStream file = entry.getValue()) {
			byte[] bytes = StreamUtils.copyToByteArray(file);
			InputStream stream = new ByteArrayInputStream(bytes);
			return new SnowballObject(entry.getKey(), stream, file.available(), null);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
}
