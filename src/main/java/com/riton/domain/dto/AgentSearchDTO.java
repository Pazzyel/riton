package com.riton.domain.dto;

import com.riton.domain.doc.ShopDoc;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentSearchDTO {
    private String content;
    private ShopDoc shopDoc;
}
