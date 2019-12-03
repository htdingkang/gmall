package com.atguigu.gmall.manage.util;

import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class PmsUploadUtil {
    public static String uploadImage(MultipartFile multipartFile) {

        String imgUrl="http://192.168.9.108";
        String file = PmsUploadUtil.class.getResource("/tracker.conf").getFile();
        try {
            ClientGlobal.init(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        TrackerClient trackerClient=new TrackerClient();
        TrackerServer trackerServer= null;
        try {
            trackerServer = trackerClient.getConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        StorageClient storageClient=new StorageClient(trackerServer,null);


        try {
            byte[] bytes = multipartFile.getBytes();//获取上传的二进制对象
            String originalFilename = multipartFile.getOriginalFilename();//文件全名
            //获得文件后缀名
            String extName=originalFilename.substring(originalFilename.lastIndexOf(".")+1);
            String[] upload_file = storageClient.upload_file(bytes, extName, null);

            for (int i = 0; i < upload_file.length; i++) {
                imgUrl += "/"+upload_file[i];
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return imgUrl;
    }
}
