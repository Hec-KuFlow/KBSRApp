/*
 * Copyright (c) 2022-present KuFlow S.L.
 *
 * All rights reserved.
 */
package com.kuflow.engine.samples.worker.Activity;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GSheetsActivitiesImp implements GSheetsActivities {

  private static final String APPLICATION_NAME =
    "Google Sheets API Java Quickstart";
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private static final String TOKENS_DIRECTORY_PATH = "tokens";

  /**
   * Global instance of the scopes required by this quickstart.
   * If modifying these scopes, delete your previously saved tokens/ folder.
   */
  private static final List<String> SCOPES = Collections.singletonList(
    SheetsScopes.SPREADSHEETS
  );
  private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

  /**
   * Creates an authorized Credential object.
   * @param HTTP_TRANSPORT The network HTTP Transport.
   * @return An authorized Credential object.
   * @throws IOException If the credentials.json file cannot be found.
   */
  private static Credential getCredentials(
    final NetHttpTransport HTTP_TRANSPORT
  ) throws IOException {
    // Load client secrets.
    InputStream in =
      GSheetsActivitiesImp.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
    if (in == null) {
      throw new FileNotFoundException(
        "Resource not found: " + CREDENTIALS_FILE_PATH
      );
    }
    GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
      JSON_FACTORY,
      new InputStreamReader(in)
    );

    // Build flow and trigger user authorization request.
    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
      HTTP_TRANSPORT,
      JSON_FACTORY,
      clientSecrets,
      SCOPES
    )
      .setDataStoreFactory(
        new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH))
      )
      .setAccessType("offline")
      .build();
    LocalServerReceiver receiver = new LocalServerReceiver.Builder()
      .setPort(8888)
      .build();
    return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
  }

  public final String spreadsheetId = "1N4TzejQ4pjEdb2IJbvah8pnoHx1TWYKn_jP6h42-jHE";

  @Override
  public List<String> readSheet() {
    String aCol = "";
    NetHttpTransport HTTP_TRANSPORT;
    try {
      HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
      final String range = "BUS!A1:D2";
      Sheets service = new Sheets.Builder(
        HTTP_TRANSPORT,
        JSON_FACTORY,
        getCredentials(HTTP_TRANSPORT)
      )
        .setApplicationName(APPLICATION_NAME)
        .build();
      ValueRange response = service
        .spreadsheets()
        .values()
        .get(spreadsheetId, range)
        .execute();

      List<List<Object>> values = response.getValues();

      if (values == null || values.isEmpty()) {
        System.out.println("No data found.");
      } else {
        for (List row : values) {
          // Assign data from columns A to D, which correspond to the bus/schedule/Seat table.
          aCol += String.format("|%-15s", (String) row.get(0)) + String.format("|%-15s", (String) row.get(1)) +
            String.format("|%-15s", (String) row.get(2)) + String.format("|%-15s", (String) row.get(3)) + " | \r\n";
            //Print for console feedback
            System.out.printf("|%1$-12s|%2$-10s|%3$-20s|%4$-20s\n",row.get(0),row.get(1),row.get(2),row.get(3));
        }
      }
    } catch (GeneralSecurityException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    //Add html tags for KuFlow UI Showing
    aCol = "<pre>" + aCol + "</pre>";
    return List.of(aCol);
  }

  @Override
  public List<String> writeSheet(String firstName, String lastName, String email) {
    try {
      final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
      String range = "BUS!A7:C7";
      Sheets service = new Sheets.Builder(HTTP_TRANSPORT,JSON_FACTORY,getCredentials(HTTP_TRANSPORT))
        .setApplicationName(APPLICATION_NAME).build();

      //Complete variables and write it in the last row of the spreadsheet
      List<Object> data1 = new ArrayList<>();
      data1.add(firstName);
      data1.add(lastName);
      data1.add(email);

      List<List<Object>> data = new ArrayList<>();
      data.add(data1);
      ValueRange valueRange = new ValueRange();
      valueRange.setValues(data);
      service
        .spreadsheets()
        .values()
        .append(spreadsheetId, range, valueRange)
        .setValueInputOption("USER_ENTERED")
        .execute();
    } catch (GeneralSecurityException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return List.of(firstName, lastName, email);
  }

  @Override
  public String getCellValue() {
    String cell = "";
    try {
      final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
      final String range = "BUS!D2";
      Sheets service = new Sheets.Builder(HTTP_TRANSPORT,JSON_FACTORY,getCredentials(HTTP_TRANSPORT))
        .setApplicationName(APPLICATION_NAME).build();

      ValueRange response = service.spreadsheets().values().get(spreadsheetId, range).execute();

      List<List<Object>> values = response.getValues();
      if (values == null || values.isEmpty()) {
        System.out.println("No data found.");
      } else {
        for (List row : values) {
          cell = (String) row.get(0);
        }
      }
    } catch (GeneralSecurityException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return cell;
  }

  @Override
  public String getSeatNo() {
    // Declare a variable for the seat number
    String seatNo = "";
    try {
      final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

      Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY,getCredentials(HTTP_TRANSPORT))
        .setApplicationName(APPLICATION_NAME)
        .build();
      final String range = "BUS!B7:B18";
      ValueRange response = service.spreadsheets().values().get(spreadsheetId, range).execute();

      //Count the amount of rows ocuppied
      seatNo =String.valueOf(service.spreadsheets().values().get(spreadsheetId, range).execute().getValues().size());

    } catch (GeneralSecurityException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return seatNo;
  }
}
