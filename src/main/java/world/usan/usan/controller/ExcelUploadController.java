package world.usan.usan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ExcelUploadController {

    @RequestMapping("/excel-upload")
    public String excelUpload() {

        return "pages/excel-upload";
    }
}
