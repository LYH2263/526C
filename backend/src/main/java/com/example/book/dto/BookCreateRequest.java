package com.example.book.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BookCreateRequest {

    @NotBlank(message = "书名不能为空")
    private String title;

    @NotBlank(message = "作者不能为空")
    private String author;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须为正数")
    private BigDecimal price;

    @PastOrPresent(message = "出版日期不能是未来日期")
    private LocalDate publishDate;

    private String description;
}
