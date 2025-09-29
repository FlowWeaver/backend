package site.icebang.domain.auth.mapper;

import org.apache.ibatis.annotations.Mapper;

import site.icebang.domain.auth.dto.RegisterDto;
import site.icebang.domain.auth.model.AuthCredential;

@Mapper
public interface AuthMapper {
  AuthCredential findUserByEmail(String email);

  boolean existsByEmail(String email);

  int insertUser(RegisterDto dto); // users insert

  int insertUserOrganization(RegisterDto dto); // user_organizations insert

  int insertUserRoles(RegisterDto dto); // user_roles insert (foreach)

  String findPasswordByEmail(String email);

  int updatePassword(String email, String newPassword);
}
