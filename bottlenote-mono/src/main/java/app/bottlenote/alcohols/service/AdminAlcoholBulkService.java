package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.dto.request.AdminAlcoholBulkRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholBulkCreateResponse;
import app.bottlenote.alcohols.dto.response.AdminAlcoholBulkValidateResponse;

public interface AdminAlcoholBulkService {
  AdminAlcoholBulkValidateResponse validate(AdminAlcoholBulkRequest request);

  AdminAlcoholBulkCreateResponse create(AdminAlcoholBulkRequest request);
}
