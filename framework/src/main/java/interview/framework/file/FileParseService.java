package interview.framework.file;

import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.microsoft.OfficeParserConfig;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.io.InputStream;


/**
 * @author zhuxi
 */
@Slf4j
@Service
public class FileParseService {

    private static final int MAX_TEXT_LENGTH = 10 * 1024 * 1024;

    public String parseText(MultipartFile file) {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(MAX_TEXT_LENGTH);
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();

        context.set(Parser.class, parser);
        context.set(EmbeddedDocumentExtractor.class, new EmbeddedDocumentExtractor() {
            @Override
            public boolean shouldParseEmbedded(Metadata metadata) {
                return false;
            }
            @Override
            public void parseEmbedded(InputStream inputStream, ContentHandler contentHandler, Metadata metadata, boolean b) {
            }
        });


        OfficeParserConfig officeParserConfig = new OfficeParserConfig();
        officeParserConfig.setIncludeHeadersAndFooters(false);
        officeParserConfig.setUseSAXPptxExtractor(false);
        officeParserConfig.setExtractAllAlternativesFromMSG(false);
        context.set(OfficeParserConfig.class, officeParserConfig);

        PDFParserConfig pdfConfig = new PDFParserConfig();
        pdfConfig.setExtractInlineImages(false);
        pdfConfig.setSortByPosition(true);
        pdfConfig.setEnableAutoSpace(true);
        pdfConfig.setSuppressDuplicateOverlappingText(true);
        pdfConfig.setExtractAnnotationText(false);
        pdfConfig.setExtractUniqueInlineImagesOnly(false);
        context.set(PDFParserConfig.class, pdfConfig);

        try (InputStream inputStream = file.getInputStream()) {
            parser.parse(inputStream, handler, metadata, context);
            return normalizeWhitespace(handler.toString());
        } catch (TikaException | SAXException | IOException e) {
            log.error("文件文本提取失败, fileName={}", file.getOriginalFilename(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private String normalizeWhitespace(String text) {
        // 1. 统一所有换行符为 \n
        text = text.replaceAll("\\R+", "\n");
        // 2. 压缩多个连续空行为一个空行（可根据需要保留一个段落间距）
        text = text.replaceAll("\n{3,}", "\n\n");
        // 3. 去除首尾空白
        return text.strip();
    }
}
