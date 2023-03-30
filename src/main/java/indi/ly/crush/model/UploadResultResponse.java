package indi.ly.crush.model;

import io.minio.ObjectWriteResponse;

import java.io.Serial;
import java.io.Serializable;

/**
 * <h2>上传结果响应</h2>
 *
 * @author 云上的云
 * @since 1.0
 */
public class UploadResultResponse
		implements Serializable {
	@Serial
	private static final long serialVersionUID = -6849794470754667710L;
	/**
	 * <p>
	 *     一个可访问 MinIO 服务器上文件的完整 URL 路径字符串, 用户可以凭此访问服务器资源(<em>用户自己所上传的文件</em>).
	 * </p>
	 */
	private String fileAccessUrl;
	private ObjectWriteResponse response;
	
	public UploadResultResponse(String fileAccessUrl, ObjectWriteResponse response) {
		this.fileAccessUrl = fileAccessUrl;
		this.response = response;
	}
	
	public String getFileAccessUrl() {
		return fileAccessUrl;
	}
	
	public void setFileAccessUrl(String fileAccessUrl) {
		this.fileAccessUrl = fileAccessUrl;
	}
	
	public ObjectWriteResponse getResponse() {
		return response;
	}
	
	public void setResponse(ObjectWriteResponse response) {
		this.response = response;
	}
	
	@Override
	public String toString() {
		return "UploadResultResponse{" + "fileAccessUrl='" + fileAccessUrl + '\'' + ", response=" + response + '}';
	}
}
