/*
 * Copyright (c) 2022-present KuFlow S.L.
 *
 * All rights reserved.
 */
package com.kuflow.engine.samples.worker.activity;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GSheetsActivitiesImpl implements GSheetsActivities {

    private static final Logger LOGGER = LoggerFactory.getLogger(GSheetsActivitiesImpl.class);

    private static final String APPLICATION_NAME = "Google Sheets API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    public static final String SPREAD_SHEET_ID = "1N4TzejQ4pjEdb2IJbvah8pnoHx1TWYKn_jP6h42-jHE";

    /**
     * Creates an authorized Credential object.
     * @param httpTransport The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport httpTransport) throws IOException {
        // Load client secrets.
        try (InputStream in = GSheetsActivitiesImpl.class.getResourceAsStream(CREDENTIALS_FILE_PATH)) {
            if (in == null) {
                throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
            }
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

            // Build flow and trigger user authorization request.
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
                JSON_FACTORY,
                clientSecrets,
                SCOPES
            )
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();

            return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        }
    }

    @Override
    public List<String> readSheet() {
        StringBuilder tableData = new StringBuilder();
        String textFormat = "|%-15s";
        NetHttpTransport HTTP_TRANSPORT;
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            final String range = "BUS!A1:D2";
            Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
            ValueRange response = service.spreadsheets().values().get(SPREAD_SHEET_ID, range).execute();

            List<List<Object>> values = response.getValues();

            if (values == null || values.isEmpty()) {
                System.out.println("No data found.");
            } else {
                for (List<Object> row : values) {
                    // Assign data from columns A to D, which correspond to the bus/schedule/Seat table.
                    tableData
                        .append(String.format(textFormat, row.get(0)))
                        .append(String.format(textFormat, row.get(1)))
                        .append(String.format(textFormat, row.get(2)))
                        .append(String.format(textFormat, row.get(3)))
                        .append(" | \r\n");

                    //Print for console feedback
                    LOGGER.info(
                        String.format(
                            "|%1$-12s|%2$-10s|%3$-20s|%4$-20s\n",
                            row.get(0),
                            row.get(1),
                            row.get(2),
                            row.get(3)
                        )
                    );
                }
            }
        } catch (GeneralSecurityException | IOException e) {
            LOGGER.error("Error reading sheet", e);
        }
        //Add html tags for KuFlow UI Showing
        tableData = new StringBuilder("<pre>" + tableData + "</pre>");

        return List.of(tableData.toString());
    }

    @Override
    public List<String> writeSheet(String firstName, String lastName, String email) {
        try {
            final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            String range = "BUS!A5:C5";
            Sheets service = new Sheets.Builder(httpTransport, JSON_FACTORY, getCredentials(httpTransport))
                .setApplicationName(APPLICATION_NAME)
                .build();

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
                .append(SPREAD_SHEET_ID, range, valueRange)
                .setValueInputOption("USER_ENTERED")
                .execute();
        } catch (GeneralSecurityException | IOException e) {
            LOGGER.error("Error writing sheet", e);
        }

        //Print for console feedback
        LOGGER.info(
            String.format("Data written in spreadsheet: %1$-20s|%2$-20s|%3$-20s|\n", firstName, lastName, email)
        );

        return List.of(firstName, lastName, email);
    }

    @Override
    public String getCellValue() {
        String range = "BUS!D2";

        String cellValue = "";
        try {
            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

            Sheets service = new Sheets.Builder(httpTransport, JSON_FACTORY, getCredentials(httpTransport))
                .setApplicationName(APPLICATION_NAME)
                .build();

            ValueRange response = service.spreadsheets().values().get(SPREAD_SHEET_ID, range).execute();

            List<List<Object>> values = response.getValues();
            if (values == null || values.isEmpty()) {
                System.out.println("No data found.");
            } else {
                for (List<Object> row : values) {
                    cellValue = (String) row.get(0);
                }
            }
        } catch (GeneralSecurityException | IOException e) {
            LOGGER.error("Error getting cell value", e);
        }

        return cellValue;
    }

    @Override
    public String getSeatNo() {
        String range = "BUS!B5:B116";

        // Declare a variable for the seat number
        String seatNo = "";
        try {
            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

            Sheets service = new Sheets.Builder(httpTransport, JSON_FACTORY, getCredentials(httpTransport))
                .setApplicationName(APPLICATION_NAME)
                .build();

            ValueRange response = service.spreadsheets().values().get(SPREAD_SHEET_ID, range).execute();

            //Count the amount of rows occupied
            seatNo = String.valueOf(response.getValues().size());
        } catch (GeneralSecurityException | IOException e) {
            LOGGER.error("Error getting seat number", e);
        }

        return seatNo;
    }
}
