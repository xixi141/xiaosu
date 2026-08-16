package com.xiaosu.controller;

import com.xiaosu.dto.DocumentDto;
import com.xiaosu.dto.IngestResult;
import com.xiaosu.service.DocumentIngestService;
import com.xiaosu.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentIngestService ingestService;
    private final DocumentService documentService;

    public DocumentController(DocumentIngestService ingestService, DocumentService documentService) {
        this.ingestService = ingestService;
        this.documentService = documentService;
    }

    @PostMapping
    public ResponseEntity<IngestResult> upload(@RequestParam("file") MultipartFile file,
                                               @RequestParam(value = "overwrite", defaultValue = "false") boolean overwrite)
            throws IOException {
        String filename = file.getOriginalFilename();
        // 防乱码入库：GBK 字节被按 UTF-8 解码的产物是 U+FFFD，入库后会污染列表展示与 RAG 引用
        if (filename != null && filename.indexOf('\uFFFD') >= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "文件名包含乱码字符，请改用 UTF-8 编码的文件名重新上传");
        }
        IngestResult result = ingestService.ingest(file.getBytes(), filename, overwrite);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public List<DocumentDto> list(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size,
                                  @RequestParam(required = false) String keyword) {
        return documentService.list(page, size, keyword);
    }

    @GetMapping("/{id}")
    public DocumentDto detail(@PathVariable Long id) {
        return documentService.detail(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
