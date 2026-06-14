package com.example.book.controller;

import com.example.book.dto.BookCreateRequest;
import com.example.book.dto.BookUpdateRequest;
import com.example.book.entity.Book;
import com.example.book.service.BookService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BookControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @SpyBean
    private BookService bookService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private BookCreateRequest buildCreateRequest() {
        BookCreateRequest req = new BookCreateRequest();
        req.setTitle("三体");
        req.setAuthor("刘慈欣");
        req.setPrice(new BigDecimal("39.00"));
        req.setPublishDate(LocalDate.of(2008, 1, 1));
        req.setDescription("中国科幻基石");
        return req;
    }

    private BookUpdateRequest buildUpdateRequest(Long id) {
        BookUpdateRequest req = new BookUpdateRequest();
        req.setId(id);
        req.setTitle("三体II：黑暗森林");
        req.setAuthor("刘慈欣");
        req.setPrice(new BigDecimal("49.00"));
        req.setPublishDate(LocalDate.of(2008, 5, 1));
        req.setDescription("黑暗森林法则");
        return req;
    }

    private List<Book> fetchAllBooks() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        return objectMapper.readValue(data.toString(), new TypeReference<List<Book>>() {});
    }

    private Long createBookAndGetId(BookCreateRequest req) throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
        List<Book> all = fetchAllBooks();
        return all.stream()
                .max(Comparator.comparing(Book::getId))
                .orElseThrow(() -> new AssertionError("创建后未找到图书记录"))
                .getId();
    }

    private Long createBookAndGetId() throws Exception {
        return createBookAndGetId(buildCreateRequest());
    }

    @Test
    @Order(1)
    @DisplayName("GET /api/books - 初始返回空列表")
    void listBooks_EmptyInitially() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/books -> GET /api/books/{id} - 新增后可被查回")
    void createBook_ThenGetById_VerifyFields() throws Exception {
        BookCreateRequest createReq = buildCreateRequest();
        Long assignedId = createBookAndGetId(createReq);

        MvcResult getResult = mockMvc.perform(get("/api/books/{id}", assignedId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").exists())
                .andReturn();

        JsonNode root = objectMapper.readTree(getResult.getResponse().getContentAsString());
        JsonNode data = root.get("data");
        assertEquals(createReq.getTitle(), data.get("title").asText());
        assertEquals(createReq.getAuthor(), data.get("author").asText());
        assertEquals(0, createReq.getPrice().compareTo(new BigDecimal(data.get("price").asText())));
        assertEquals(createReq.getPublishDate().toString(), data.get("publishDate").asText());
        assertEquals(createReq.getDescription(), data.get("description").asText());
    }

    @Test
    @Order(3)
    @DisplayName("PUT /api/books -> GET /api/books/{id} - 更新后字段变化被持久化")
    void updateBook_VerifyFieldsChanged() throws Exception {
        Long id = createBookAndGetId();

        BookUpdateRequest updateReq = buildUpdateRequest(id);
        mockMvc.perform(put("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));

        MvcResult getResult = mockMvc.perform(get("/api/books/{id}", id))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(getResult.getResponse().getContentAsString()).get("data");
        assertEquals("三体II：黑暗森林", data.get("title").asText());
        assertEquals(0, new BigDecimal("49.00").compareTo(new BigDecimal(data.get("price").asText())));
        assertEquals("黑暗森林法则", data.get("description").asText());
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/books - 新增多条后列表包含所有记录")
    void listBooks_AfterMultipleCreates() throws Exception {
        createBookAndGetId(buildCreateRequest());

        BookCreateRequest req2 = new BookCreateRequest();
        req2.setTitle("活着");
        req2.setAuthor("余华");
        req2.setPrice(new BigDecimal("25.00"));
        req2.setPublishDate(LocalDate.of(1998, 5, 1));
        createBookAndGetId(req2);

        MvcResult result = mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andReturn();

        List<Book> books = objectMapper.readValue(
                objectMapper.readTree(result.getResponse().getContentAsString()).get("data").toString(),
                new TypeReference<List<Book>>() {}
        );
        assertEquals(2, books.size());
        assertTrue(books.stream().anyMatch(b -> "三体".equals(b.getTitle())));
        assertTrue(books.stream().anyMatch(b -> "活着".equals(b.getTitle())));
    }

    @Test
    @Order(5)
    @DisplayName("DELETE /api/books/{id} -> GET /api/books/{id} - 删除后再查返回404")
    void deleteBook_ThenGetById_ReturnsNotFound() throws Exception {
        Long id = createBookAndGetId();

        mockMvc.perform(delete("/api/books/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));

        mockMvc.perform(get("/api/books/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("图书不存在，ID: " + id));
    }

    @Test
    @Order(6)
    @DisplayName("PUT /api/books - 更新不存在的 id 返回404")
    void updateBook_NonExistentId_ReturnsNotFound() throws Exception {
        BookUpdateRequest req = buildUpdateRequest(99999L);
        mockMvc.perform(put("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("图书不存在，ID: 99999"));
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/books/{id} - 查询不存在的 id 返回404")
    void getById_NonExistentId_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/books/{id}", 88888L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("图书不存在，ID: 88888"));
    }

    @Test
    @Order(8)
    @DisplayName("DELETE /api/books/{id} - 删除不存在的 id 返回404")
    void deleteById_NonExistentId_ReturnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/books/{id}", 77777L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("图书不存在，ID: 77777"));
    }

    @Test
    @Order(9)
    @DisplayName("GlobalExceptionHandler - 制造系统异常，断言 code=500 且 message 含'系统繁忙'")
    void globalExceptionHandler_SystemException_Returns500WithPrefix() throws Exception {
        Mockito.doThrow(new RuntimeException("模拟数据库连接超时"))
                .when(bookService).findAll();

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("系统繁忙"));

        Mockito.reset(bookService);
    }

    @Test
    @Order(10)
    @DisplayName("POST /api/books - 参数校验失败触发 GlobalExceptionHandler，返回400")
    void createBook_ValidationFailed_Returns400() throws Exception {
        BookCreateRequest req = new BookCreateRequest();
        req.setTitle("");
        req.setAuthor("");
        req.setPrice(new BigDecimal("-1"));

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @Order(11)
    @DisplayName("完整 CRUD 闭环 - 从创建、读取、更新、删除的全链路验证")
    void fullCrudCycle_EndToEnd() throws Exception {
        BookCreateRequest createReq = buildCreateRequest();
        Long id = createBookAndGetId(createReq);

        MvcResult get1 = mockMvc.perform(get("/api/books/{id}", id)).andReturn();
        Book b1 = objectMapper.treeToValue(
                objectMapper.readTree(get1.getResponse().getContentAsString()).get("data"), Book.class);
        assertEquals(createReq.getTitle(), b1.getTitle());

        BookUpdateRequest updateReq = buildUpdateRequest(id);
        mockMvc.perform(put("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk());

        MvcResult get2 = mockMvc.perform(get("/api/books/{id}", id)).andReturn();
        Book b2 = objectMapper.treeToValue(
                objectMapper.readTree(get2.getResponse().getContentAsString()).get("data"), Book.class);
        assertEquals("三体II：黑暗森林", b2.getTitle());

        mockMvc.perform(delete("/api/books/{id}", id))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/books/{id}", id))
                .andExpect(jsonPath("$.code").value(404));
    }
}
