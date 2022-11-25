/*
 * Copyright (c) 2022-present KuFlow S.L.
 *
 * All rights reserved.
 */
package com.kuflow.engine.samples.worker.Activity;

import io.temporal.activity.ActivityInterface;
import java.util.List;

@ActivityInterface
public interface GSheetsActivities {
  List<String> readSheet();
  List<String> writeSheet(String firstName, String lastName, String email);
  String getCellValue();
  String getSeatNo();
}
