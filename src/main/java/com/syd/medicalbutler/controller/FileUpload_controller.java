package com.syd.medicalbutler.controller;

import com.syd.medicalbutler.dto.ResultData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@RestController
public class FileUpload_controller {
    @Value("${file.uploadFolder}")
    private String uploadFolder;
    @Value("${file.staticAccessPath}")
    private String staticAccessPath;
@RequestMapping("file_upload")
    public ResultData upload(MultipartFile file)  {
        String basicdit="pics";
            String fileName = file.getOriginalFilename();
            String filePath = uploadFolder +basicdit+ "\\";
            File targetFile = new File(filePath);
            if (!targetFile.exists()) {
                targetFile.mkdirs();
            }
            try {
                String add=staticAccessPath.substring(0,staticAccessPath.length()-2);
                fileName=System.currentTimeMillis()+".png";
                file.transferTo(new File(filePath + fileName));
                 String url=add+basicdit+"/" +fileName;
                return new ResultData(1,url);
            } catch (Exception e) {
                e.printStackTrace();
                return new ResultData(-1);
            }
    }

}
