package site.icebang.domain.department.dto;

import java.math.BigInteger;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class DepartmentCardDto {
  private BigInteger id;
  private String name;
}
