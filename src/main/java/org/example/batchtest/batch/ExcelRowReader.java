package org.example.batchtest.batch;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

// 엑셀 파일의 각 행(Row)을 하나씩 읽는 Reader
public class ExcelRowReader implements ItemStreamReader<Row> {

    private final String filePath; // 엑셀 파일 경로
    private FileInputStream fileInputStream; // 파일을 열 객체
    private Workbook workbook; // 엑셀 파일 받을 객체
    private Iterator<Row> rowCursor; // 각 행 반복할 커서
    private int currentRowNumber; // 현재 읽고 있는 행 번호
    private final String CURRENT_ROW_KEY = "current.row.number"; // 배치의 메타 테이블에 어떤 행까지 했는지 저장할 키 값

    public ExcelRowReader(String filePath) throws IOException {

        this.filePath = filePath;
        this.currentRowNumber = 0;
    }

    // 엑셀 파일 열거나 초기화
    // 한 번 수행
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {

        try {
            fileInputStream = new FileInputStream(filePath);
            workbook = WorkbookFactory.create(fileInputStream);
            Sheet sheet = workbook.getSheetAt(0);
            this.rowCursor = sheet.iterator();

            // 동일 배치 파라미터에 대해 특정 키 값 "current.row.number"의 값이 존재한다면 초기화
            if (executionContext.containsKey(CURRENT_ROW_KEY)) {
                currentRowNumber = executionContext.getInt(CURRENT_ROW_KEY);
            }

            // 위의 값을 가져와 이미 실행한 부분은 건너 뜀
            for (int i = 0; i < currentRowNumber && rowCursor.hasNext(); i++) {
                rowCursor.next();
            }

        } catch (IOException e) {
            throw new ItemStreamException(e);
        }
    }

    // 한 행씩 읽음
    @Override
    public Row read() {

        if (rowCursor != null && rowCursor.hasNext()) {
            currentRowNumber++;
            return rowCursor.next();
        } else {
            return null;
        }
    }

    // 현재 읽고 있는 행 번호를 배치의 메타 테이블에 저장
    // read 처리 후, 특정 변수 값 업데이트
    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        executionContext.putInt(CURRENT_ROW_KEY, currentRowNumber);
    }

    // 배치 작업 후, 열었던 리소스 닫기
    @Override
    public void close() throws ItemStreamException {

        try {
            if (workbook != null) {
                workbook.close();
            }
            if (fileInputStream != null) {
                fileInputStream.close();
            }
        } catch (IOException e) {
            throw new ItemStreamException(e);
        }
    }
}