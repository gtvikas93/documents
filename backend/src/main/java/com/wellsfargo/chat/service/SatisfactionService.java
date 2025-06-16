package com.wellsfargo.chat.service;

import com.wellsfargo.chat.model.SatisfactionFeedback;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

@Service
public class SatisfactionService {
    private final String excelFilePath;

    public SatisfactionService(@Value("${satisfaction.excel.path:feedback.xlsx}") String excelFilePath) {
        this.excelFilePath = excelFilePath;
        initializeExcelFile();
    }

    private void initializeExcelFile() {
        File file = new File(excelFilePath);
        if (!file.exists()) {
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Feedback");
                
                // Create header row
                Row headerRow = sheet.createRow(0);
                String[] headers = {"CustomerId", "ECN", "XAID", "OriginalPromptMessage", "SessionId", "Timestamp", "SatisfactoryMessage", "Reason"};
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                }

                // Write to file
                try (FileOutputStream fileOut = new FileOutputStream(excelFilePath)) {
                    workbook.write(fileOut);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to initialize feedback Excel file", e);
            }
        }
    }

    public void saveFeedback(SatisfactionFeedback feedback) {
        try (FileInputStream fis = new FileInputStream(excelFilePath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();
            Row row = sheet.createRow(lastRowNum + 1);

            // Add data to row
            row.createCell(0).setCellValue(feedback.getCustomerId());
            row.createCell(1).setCellValue(feedback.getEcn());
            row.createCell(2).setCellValue(feedback.getXaId());
            row.createCell(3).setCellValue(feedback.getOriginalPromptMessage());
            row.createCell(4).setCellValue(feedback.getSessionId());
            row.createCell(5).setCellValue(feedback.getTimestamp());
            row.createCell(6).setCellValue(feedback.getSatisfactoryMessage());
            row.createCell(7).setCellValue(feedback.getReason());

            // Write to file
            try (FileOutputStream fileOut = new FileOutputStream(excelFilePath)) {
                workbook.write(fileOut);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save feedback", e);
        }
    }
} 