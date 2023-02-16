# KuFlow - Third-Party API (Google GSheets) example

# What we will create?

This tutorial will guide us in building a simple Temporal.io worker (*when we apply the Workflow as Code paradigm*) workflow, connecting to a new 3rd Party API (**Google**) to read and write some information from a spreadsheet.
Our use case will be a bus seat reservation process in which we will get the availability of seats (*reading the spreadsheet*), if spaces are available, we'll fill out a form (*in KuFlow’s UI*), and the workflow will complete the reservation process (*writing in the spreadsheet*), and notify us of the outcome. If there are no seats available, we'll be informed without the need to complete the form (*both cases through the Kuflow’s UI*).

# Prerequisites

Before starting our Serverless Workflow for the first time, we must register it in [KuFlow (app.kuflow.com)](https://app.kuflow.com/). After familiarizing yourself with the user interface, you are ready to perform the necessary configurations for our Worker. To do so, click on the `Settings` menu. If you do not see this menu, you probably do not have the necessary permissions to define processes, so you will have to request it from your Tenant administrators, or simply create a FREE new account.

## Create the Credentials

### 3rd Party API (Google) Credentials

To use this 3rd party Application you must create a Google Dev Project and credentials following the Google Developer Platform (*https://developers.google.com/sheets/api/quickstart/java*).
### Create the credentials for the Worker​

We will configure an `APPLICATION` that will provide us with the necessary credentials so that our Worker (written in Java and located in your own machine) can interface with KuFlow.

Go to the `Settings > Applications` menu and click on `Add application`. We establish the name we want and save. Next, you will get the first data needed to configure our Worker.

- **Identifier**: Unique identifier of the application. For this tutorial: *myApp*
  - Later in this tutorial, we will configure it in the `kuflow.api.application-id` property of our example.
- **Token**: Password for the application.
  - Later in this tutorial, we will configure it in the `kuflow.api.token` property of our example.
- **Namespace**: Temporal's namespace.
  - Later in this tutorial, we will configure it in the `application.temporal.namespace` property of our example.

Next, we will proceed to create the certificates that will serve us to configure the Mutual TLS with which our Worker will perform the authentication against Temporal. To do this we click on "Add certificate", set the name we want, and choose the `PKCS8` type for the encryption of the private key. This is important since the example code in this tutorial works with this encoding. We will get the following:

- **Certificate**: It is the public part that is presented in the *mTLS* connection.
  - Later in this tutorial, we will configure it in the `application.temporal.mutual-tls.cert-data` property of our example.
- **Private Key**: It is the private key for *mTLS*.
  - Later in this tutorial, we will configure it in the `application.temporal.mutual-tls.key-data` property of our example.

It is also necessary to indicate the CA certificate, which is the root certificate with which all certificates are issued. It is a public certificate and for convenience you can find it in the same `Application` screen, under the name of *CA Certificate*. This certificate will always be the same between workers.

- **CA Certificate**: Root certificate with which all certificates (client and server certificates) are issued.
  - Later in this tutorial, we will configure it in the `kuflow.activity.kuflow.key-data` property of our example.

Finally, you get something like:

<div class="center">

![](/img/TUT03-01-App.png)

</div>

## Preparing Scenario

### Create the spreadsheet

Create a new spreadsheet in a GDrive folder as follows:

- Table one, range A1:D4 with the Bus Billboard where we will read the information to show in our process.
- Table two, Range A4:C16 with a header and where the passenger info will be written.

**NOTE:** For simplification purposes, the only cell with formula is D2: **=C2 - COUNTIF(A5:A16;"<>")**

Getting something like this:

<div class="center">

![](/img/TUT03-02-GSpreadsheet.png)

</div>

### Create the process definition

We need to create the definition of the process that will execute our Workflow. In this section, we will configure the KuFlow tasks of which it is made up as well as the information necessary to complete said tasks, the process access rules (i.e. *RBAC*), as well as another series of information. To do this we go to the `Setting > Processes` menu and create a new process.

Complete *Process Definition* with the following data:

- Process name
	- Bus Seat Reservation
- Description
	- Free text description about the Workflow.
- Workflow
	- Workflow Engine
		- *KuFlow Engine*, because we are designing a Temporal-based Worker.
  - Workflow Application
myApp, the application to which our Worker will connect to.
	- Task queue
		- The name of the Temporal queue where the KuFlow tasks will be set. You can choose any name, later you will set this same name in the appropriate configuration in your Worker. For this tutorial: BSRQueue.
	- Type
		- It must match the name of the Java interface of the Workflow. For this tutorial, BSRWorker is the name you should type in this input.
- Permissions
	- At least one user or group of users must have the role of `INITIATOR` to instantiate the process through the application. In this tutorial, we will allow the *“Default Group”* from this organization.

Finally, you get something like:

<div class="center">

![](/img/TUT03-03-Process.png)

</div>

We will define three **Tasks Definitions** in the process as follows:

- Task one **"Reservation Form"**
	- **Description:** Free text description about the Task.
	- **Code:** BSR_FRM
	- **Candidates:** Default Group
	- **Elements:**
		- **Name:** Seats
			- **Description:** Free text description about the element (not mandatory).
			- **Code:** seats
			- **Type:** Field
			- **Properties:** Read Only
			- **Field Type:** Text, Multiline checked
		- **Name:** First Name 
			- **Description:** Free text description about the element (not mandatory).
			- **Code:** firstName
			- **Type:** Field
			- **Properties:** Mandatory
			- **Field Type:** Text
				- **Validations:** Length Greater than 0
		- **Name:** Last Name 
			- **Description:** Free text description about the element (not mandatory).
			- **Code**: lastName
			- **Type**: Field
			- **Properties**: Mandatory
			- **Field Type:** Text
				- **Validations**: Length Greater than 0
		- **Name**: Email 
			- **Description**: Free text description about the element (not mandatory).
			- **Code**: email
			- **Type**: Field
			- **Properties**: Mandatory
			- **Field Type**: Text
				- **Validations**: Email

- Task two **"Notification: No seats available"**
	- **Description**: Free text description about the Task.
	- **Code**: NOTIF_NOSEAT
	- **Candidates**:Default Group
	- No **Elements** Needed:

- Task three **"Notification: Reservation complete"**
	- **Description**: Free text description about the Task.
	- **Code**: NOTIF_RSVCP
	- **Candidates**: Default Group
	- **Elements**:
		- **Name**: Seat Number
		- **Description**: Free text description about the element (not mandatory).
		- **Code**: seatNo
		- **Type**: Field
		- **Properties**: Read Only
		- **Field Type**: Text

You'll get something like:

<div class="text--center">

![](/img/TUT03-04-Process.png)

</div>

### Publish the process and download the template for the Workflow Worker​

By clicking on the “Publish” button you’ll receive a confirmation request message, once you have confirmed the process will be published.

<div class="center">

![](/img/TUT03-05-publish.png)

![](/img/TUT03-06-publish.png)

</div>

Now, you can download a sample Workflow Implementation from the Process Definition main page.

<div class="center">

![](/img/TUT03-07-Template_1.png)

![](/img/TUT03-07-Template_2.png)

</div>

This code will serve as a starting point for implementing our Loan Application worker. The requirements for its use are the following:

- **Java JDK**
  - You need to have a Java JDK installed on your system. The current example code uses version 17, but is not required for the KuFlow SDK. You can use for example [Adoptium](https://adoptium.net/) distribution or any other. We recommend you to use a tool like [SDKMan](https://sdkman.io/jdks) to install Java SDK distributions in a comfortable way.
- **IDE**
  - An IDE with good Java support is necessary to work comfortably. You can use *VSCode*, *IntelliJ Idea,* *Eclipse* or any other with corresponding Java plugins.

### Main technologies used in the example

To make things simpler, the following technologies have been mainly used in our example:

- **Maven**
  - To build the example. It is distributed in an integrated way so it is not necessary to install it in isolation.
- **Spring Boot and Spring Cloud**
  - To wire all our integrations.
- **Temporal Java SDK**
  - To perform GRPC communications with the KuFlow temporal service.
- **OpenFeign**
  - To implement the KuFlow API Rest client.

## Implementation

*Note:* You can download the following project from our [public Github repository](https://github.com/kuflow/kuflow-engine-samples-java), be sure to add all the tokens and secrets from your KuFlow account and 3rd party API developers.

### Resolve dependencies​

We need to modify pom.xml, to include new dependencies:

```xml
   <dependency>
      <groupId>com.google.api-client</groupId>
      <artifactId>google-api-client</artifactId>
      <version>1.33.0</version>
    </dependency>
    <dependency>
      <groupId>com.google.oauth-client</groupId>
      <artifactId>google-oauth-client-jetty</artifactId>
      <version>1.32.1</version>
    </dependency><dependency>
      <groupId>com.google.apis</groupId>
      <artifactId>google-api-services-sheets</artifactId>
      <version>v4-rev20210629-1.32.1</version>
    </dependency>
  ```

### Using Credentials

Now, in this step we are filling up the application configuration information. You must complete all the settings and replace the example values.

#### KuFlow’s Credentials

The appropriate values can be obtained from the KuFlow application. Check out the [Create the credentials] section of this tutorial.

```yaml
# ===================================================================
# PLEASE COMPLETE ALL CONFIGURATIONS BEFORE STARTING THE WORKER
# ===================================================================

kuflow:
  api:

    # ID of the APPLICATION configured in KUFLOW.
    # Get it in "Application details" in the Kuflow APP.
    application-id: FILL_ME

    # TOKEN of the APPLICATION configured in KUFLOW.
    # Get it in "Application details" in the Kuflow APP.
    token: FILL_ME

application:
  temporal:
    # Temporal Namespace. Get it in "Application details" in the KUFLOW APP.
    namespace: FILL_ME

    # Temporal Queue. Configure it in the "Process definition" in the KUFLOW APP.
    kuflow-queue: FILL_ME

    mutual-tls:

      # Client certificate
      # Get it in "Application details" in the KUFLOW APP.
      cert-data: |
        -----BEGIN CERTIFICATE-----
        fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_
        fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_
        fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_
        ...
        fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_
        fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_
        fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_
        -----END CERTIFICATE-----


      # Private key
      # Get it in "Application details" in the KUFLOW APP.
      # IMPORTANT: This example works with PKCS8, so ensure PKCS8 is selected
      #            when you generate the certificates in the KUFLOW App
      key-data: |
        -----BEGIN PRIVATE KEY-----
        fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_
        fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_
        fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_
        ...
        fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_
        fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_
        fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_
        -----END PRIVATE KEY-----

      # KUFLOW Certification Authority (CA) of the certificates issued in KUFLOW
      ca-data: |
        -----BEGIN CERTIFICATE-----
        fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_
        fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_
        fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_
        ...
        fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_
        fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_
        fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_fill_me_
        -----END CERTIFICATE-----
```

Please note that this is a YAML, respect the indentation.

<div class="text--center">

![](/img/TUT03-07-Template_3.png)

</div>

#### 3rd Party (Google) Credentials

We create a new file called **credentials.json** inside a subfolder **/resources**, with the content generated from the Google Prerequisites​ section on this tutorial.

<div class="text--center">

![](/img/TUT03-07-Template_4.png)

</div>

### Define new Activities ​

We create a new subfolder called **activity** and inside a file called **GSheetsActivities.java** with the following content:

```java
**/*
 * Copyright (c) 2022-present KuFlow S.L.
 *
 * All rights reserved.
 */
package com.kuflow.engine.samples.worker.activity;

import io.temporal.activity.ActivityInterface;
import java.util.List;

@ActivityInterface
public interface GSheetsActivities {
    List<String> readSheet();
    List<String> writeSheet(String firstName, String lastName, String email);
    String getCellValue();
    String getSeatNo();
}
```

We also have to create **GSheetsActivitiesImpl.java** which is the implementation of the previous activities, using the 3rd party API methods to connect, read and write.

In summary, the next piece of code will contain the basics methods from  [Java Quickstart](https://developers.google.com/sheets/api/quickstart/java) modified for our use case.

- **getCredentials()**: Using tokens given by Google, we'll establish a new session.
- **readSheet()**: Given the range of table one **"Bus billboard"** ("*BUS!A1:D2*") we return the information on it.
- **writeSheet(Args...	)**: Once the form is completed, the information will be given to this method to write on the spreadsheet.
- **getCellValue()**: Given the range of one cell ("*BUS!D4*") we return the information on it.
- **getSeatNo()**: This method will count the rows on the given range ("*BUS!B5:B116*") count the rows which contain text and return it.
  
```java
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

    public static final String SPREAD_SHEET_ID = "FILL_ME";
    public static final String SHEET_NAME = "BUS!";

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
            final String range = SHEET_NAME + "A1:D2";
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
            String range = SHEET_NAME + "A5:C5";
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
        String range = SHEET_NAME + "D2";

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
        String range = SHEET_NAME + "B5:B116";

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
```

### Register Activities

In order to register the Twitter activity in our worker, we must modify **TemporalBootstrap.java** as follows. *//HERE before each line to highlight new code.*
We declare the instance variable after *kuFlowActivities*:

```java
private final KuFlowActivities kuflowActivities;
//HERE
private final GSheetsActivities gSheetsActivities;
```

We modify the constructor as follows:

```java
public TemporalBootstrap(
    ApplicationProperties applicationProperties,
    WorkerFactory factory,
    KuFlowActivities kuflowActivities,
    //HERE
    GSheetsActivities gSheetsActivities
  ) {
    this.applicationProperties = applicationProperties;
    this.factory = factory;
    this.kuflowActivities = kuflowActivities;
    //HERE
    this.gSheetsActivities = gSheetsActivities;
  }
```

and we add the following line to register the activity in *startWorkers()*

```java
    worker.registerActivitiesImplementations(
      this.kuflowActivities,
      //HERE
      this.gSheetsActivities
    );
```

### Workflow Implementation

In this section, we will make the fundamental steps to creating the most basic workflow for this business process:

- Users in the organization could get the availability of seats in a Bus, and if spaces are available, fill out a form to book a seat and receive a book confirmation with the seat number, and if there are no seats available, be informed without the need to complete the form.

Open the **SampleWorkflowImpl.java** file and modify it as follows. *//HERE before each line to highlight new code.*

Declaring an instance of our new activities after kuflowActivities declaration:

```java
private final KuFlowActivities kuflowActivities;
//HERE
private final GSheetsActivities gSheetsActivities;
```

At the end of the constructor method **SampleWorkflowImpl()** add the following to initialize the activities:

```java
this.kuflowActivities =
Workflow.newActivityStub(
                KuFlowActivities.class,
                defaultActivityOptions,
                Map.of(                         TemporalUtils.getActivityType(KuFlowActivities.class, 
"createTaskAndWaitFinished"), asyncActivityOptions
                )
            );
//HERE
this.gSheetsActivities = Workflow.newActivityStub(GSheetsActivities.class, defaultActivityOptions);
    }
```

Now we modify the **runWorkflow()** method as follows to:

1. Assign to a variable the cell value returned from our method **getCellValue()**.
2. If no seats are available, create and execute the task for *"No Seats"* notification.
3. If a seat is available, create and execute the task to show the Billboard and the form.
4. Use the **writeSheet()** method with the information filled.
5. Create and execute the task for *"Reservation Complete"* notification, showing the seat number assigned.

```java
@Override
    public WorkflowResponseResource runWorkflow(WorkflowRequestResource request) {
        LOGGER.info("Process {} started", request.getProcessId());

        this.kuFlowGenerator = new KuFlowGenerator(request.getProcessId());

        //1
        String seatsAvailable = this.gSheetsActivities.getCellValue();
        if ((seatsAvailable).equalsIgnoreCase("0")) {
            //2
            this.createTaskNotificationNoSeatsAvailable(request);
        } else {
            //3
            TaskResource taskReservationApplication = this.createTaskReservationForm(request);
            //4
            this.writeSheet(taskReservationApplication);
            //5
        This.createTaskNotificationReservationComplete(request);
        }

        CompleteProcessResponseResource completeProcess = this.completeProcess(request.getProcessId());

        LOGGER.info("Process {} finished", request.getProcessId());

        return this.completeWorkflow(completeProcess);
    }
```

Next, we add in **createTaskReservationForm()** the following lines of code, to Adding the reading of the sheet and passing it to the text element “seats”

```java
private TaskResource createTaskReservationForm(WorkflowRequestResource workflowRequest) {
        UUID taskId = this.kuFlowGenerator.randomUUID();

        CreateTaskRequestResource createTaskRequest = new CreateTaskRequestResource();
        createTaskRequest.setTaskId(taskId);
        createTaskRequest.setTaskDefinitionCode(TASK_CODE_RESERVATION_FORM);
        createTaskRequest.setProcessId(workflowRequest.getProcessId());

        //HERE
        List<String> result = this.gSheetsActivities.readSheet();
        createTaskRequest.putElementValuesItem("seats", TaskElementValueWrapperResource.of(result.get(0)));

        // Create and retrieve Task in KuFlow
        this.kuflowActivities.createTaskAndWaitFinished(createTaskRequest);

        RetrieveTaskRequestResource retrieveTaskRequest = new RetrieveTaskRequestResource();
        retrieveTaskRequest.setTaskId(taskId);
        RetrieveTaskResponseResource retrieveTaskResponse = this.kuflowActivities.retrieveTask(retrieveTaskRequest);

        return retrieveTaskResponse.getTask();
    }
```

Something similar will be done in **createTaskNotificationReservationComplete()** this time to use the **getSeatNo()** method:

```java
String seatNo = this.gSheetsActivities.getSeatNo();
createTaskRequest.putElementValuesItem("seatNo", TaskElementValueWrapperResource.of(seatNo));
```

The last modification will be a creation of a method called writeSheet() to get the information from the completed form and given to the **writeSheet()** Method:

```java
private List<String> writeSheet(TaskResource task) {
        String firstName = task.getElementValues().get("firstName").getValueAsString();
        String lastName = task.getElementValues().get("lastName").getValueAsString();
        String email = task.getElementValues().get("email").getValueAsString();

        return this.gSheetsActivities.writeSheet(firstName, lastName, email);
    }
```

The final step with the code is including some imports needed for this tutorial using some feature of your IDE (like pressing SHIFT+ ALT + O in Visual Studio Code).

## Testing

We can test all that we have done by running  the worker (like pressing F5 in Visual Studio Code):

<div class="text--center">

![](/img/TUT03-08-Test_1.png)

</div>

And initiating the process in KuFlow’s UI.

<div class="text--center">

![](/img/TUT03-08-Test_2.png)

</div>

**Note:** Maybe the 3rd Party API request authorization for access, please follow the indications:

<div class="text--center">

![](/img/TUT03-08-Test_3.png)

</div>

If there are seats available:

<div class="text--center">

![](/img/TUT03-08-Test_4.png)

</div>

the UI will show the Bus Billboard and the form to complete with the booking information.

<div class="text--center">

![](/img/TUT03-08-Test_5.png)
![](/img/TUT03-08-Test_6.png)
![](/img/TUT03-08-Test_7.png)

</div>

And we see that the data is entered in the spreadsheet
<div class="text--center">

![](/img/TUT03-08-Test_8.png)

</div>

If not, because Seats Available = 0, will show the “No Seats Available” notification:

<div class="text--center">

![](/img/TUT03-08-Test_9.png)

</div>

You get something like this:

<div class="text--center">

![](/img/TUT03-08-Test_10.png)

</div>

## Summary

In this tutorial, we have covered the basics of creating a Temporal.io based workflow in KuFlow using a 3rd Party API (Google). We have defined a new process definition and we have built a workflow that contemplates the following business rules involving human tasks:

1. Read a Google Spreadsheet
2. Show this information in KuFlow’s UI
3. Get information from KuFlow’s UI and write it into the Spreadsheet.
4. Notify the requester of the response.

We have created a special video with the entire process:

Here you can watch all steps in the following video:

<a href="https://youtu.be/nTLGa2zheF0" target="_blank" title="Play me!">
  <p align="center">
    <img width="75%" src="https://img.youtube.com/vi/nTLGa2zheF0/maxresdefault.jpg" alt="Play me!"/>
  </p>
</a>

We sincerely hope that this step-by-step guide will help you to understand better how KuFlow can help your business to have better and more solid business processes.
