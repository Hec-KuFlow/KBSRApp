/*
 * Copyright (c) 2021-present KuFlow S.L.
 *
 * All rights reserved.
 */

package com.kuflow.engine.samples.worker;

import com.kuflow.engine.client.activity.kuflow.KuFlowActivities;
import com.kuflow.engine.client.activity.kuflow.resource.CompleteProcessRequestResource;
import com.kuflow.engine.client.activity.kuflow.resource.CompleteProcessResponseResource;
import com.kuflow.engine.client.activity.kuflow.resource.CreateTaskRequestResource;
import com.kuflow.engine.client.activity.kuflow.resource.RetrieveTaskRequestResource;
import com.kuflow.engine.client.activity.kuflow.resource.RetrieveTaskResponseResource;
import com.kuflow.engine.client.common.KuFlowGenerator;
import com.kuflow.engine.client.common.resource.WorkflowRequestResource;
import com.kuflow.engine.client.common.resource.WorkflowResponseResource;
import com.kuflow.engine.client.common.util.TemporalUtils;
import com.kuflow.engine.samples.worker.Activity.GSheetsActivities;
import com.kuflow.rest.client.resource.TaskElementValueWrapperResource;
import com.kuflow.rest.client.resource.TaskResource;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;

public class SampleWorkflowImpl implements SampleWorkflow {

    private static final Logger LOGGER = Workflow.getLogger(SampleWorkflowImpl.class);

    private static final String TASK_CODE_RESERVATION_FORM = "BSR_FRM";
    private static final String TASK_CODE_NOTIFICATION_NO_SEATS_AVAILABLE = "NOTIF_NOSEAT";
    private static final String TASK_CODE_NOTIFICATION_RESERVATION_COMPLETE = "NOTIF_RSVCP";

    private final KuFlowActivities kuflowActivities;
    //Declaring the instance of the activity
    private final GSheetsActivities gSheetsActivities;

    private KuFlowGenerator kuFlowGenerator;

    public SampleWorkflowImpl() {
        RetryOptions defaultRetryOptions = RetryOptions.newBuilder().validateBuildWithDefaults();

        ActivityOptions defaultActivityOptions = ActivityOptions
            .newBuilder()
            .setRetryOptions(defaultRetryOptions)
            .setStartToCloseTimeout(Duration.ofMinutes(10))
            .setScheduleToCloseTimeout(Duration.ofDays(365))
            .validateAndBuildWithDefaults();

        ActivityOptions asyncActivityOptions = ActivityOptions
            .newBuilder()
            .setRetryOptions(defaultRetryOptions)
            .setStartToCloseTimeout(Duration.ofDays(1))
            .setScheduleToCloseTimeout(Duration.ofDays(365))
            .validateAndBuildWithDefaults();

        this.kuflowActivities =
            Workflow.newActivityStub(
                KuFlowActivities.class,
                defaultActivityOptions,
                Map.of(TemporalUtils.getActivityType(KuFlowActivities.class, "createTaskAndWaitFinished"), asyncActivityOptions)
            );
        //Add here this line
        this.gSheetsActivities = Workflow.newActivityStub(GSheetsActivities.class, defaultActivityOptions);
    }

    /**Pregunta a JORGE/ZEBEN
     * esto conviene dejarlo aqui o deberiamos ponerlo en otro lado?
     */
    private List<String> writeSheet(TaskResource task){
        String firstName = task.getElementValues().get("firstName").getValueAsString();
        String lastName = task.getElementValues().get("lastName").getValueAsString();       
        String email = task.getElementValues().get("email").getValueAsString();

        return this.gSheetsActivities.writeSheet(firstName, lastName, email);
    }

    @Override
    public WorkflowResponseResource runWorkflow(WorkflowRequestResource request) {
        LOGGER.info("Process {} started", request.getProcessId());

        this.kuFlowGenerator = new KuFlowGenerator(request.getProcessId());

        /**BUSINESS LOGIC
         * Get seats available and if there's room show the form to be completed. 
         * If not, the "no seats available notification" will be shown.
         * Once the form was completed, the user's information will be written in the 
         * spreadsheet and inform the seat number through the "Reservation completed notification"
         */
        String seatsAvailable = this.gSheetsActivities.getCellValue();
        
        if ((seatsAvailable).equalsIgnoreCase("0")){
            
            this.createTaskNotificationNoSeatsAvailable(request);

        } else{

            TaskResource taskReservationApplication = this.createTaskReservationForm(request);
            this.writeSheet(taskReservationApplication);
            this.createTaskNotificationReservationComplete(request);

        }
        
        CompleteProcessResponseResource completeProcess = this.completeProcess(request.getProcessId());

        LOGGER.info("Process {} finished", request.getProcessId());

        return this.completeWorkflow(completeProcess);
    }

    private WorkflowResponseResource completeWorkflow(CompleteProcessResponseResource completeProcess) {
        WorkflowResponseResource workflowResponse = new WorkflowResponseResource();
        workflowResponse.setMessage(completeProcess.getMessage());

        return workflowResponse;
    }

    private CompleteProcessResponseResource completeProcess(UUID processId) {
        CompleteProcessRequestResource request = new CompleteProcessRequestResource();
        request.setProcessId(processId);

        return this.kuflowActivities.completeProcess(request);
    }

    /**
     * Create task "Reservation Form" in KuFlow and wait for its completion
     *
     * @param workflowRequest workflow request
     * @return task created
     */
    private TaskResource createTaskReservationForm(WorkflowRequestResource workflowRequest) {
        UUID taskId = this.kuFlowGenerator.randomUUID();

        CreateTaskRequestResource createTaskRequest = new CreateTaskRequestResource();
        createTaskRequest.setTaskId(taskId);
        createTaskRequest.setTaskDefinitionCode(TASK_CODE_RESERVATION_FORM);
        createTaskRequest.setProcessId(workflowRequest.getProcessId());

        //Adding the reading of the sheet and passing it to the text element
        List <String> result = this.gSheetsActivities.readSheet();
        createTaskRequest.putElementValuesItem("seats", TaskElementValueWrapperResource.of(result.get(0)));
        
        // Create and retrieve Task in KuFlow
        this.kuflowActivities.createTaskAndWaitFinished(createTaskRequest);

        RetrieveTaskRequestResource retrieveTaskRequest = new RetrieveTaskRequestResource();
        retrieveTaskRequest.setTaskId(taskId);
        RetrieveTaskResponseResource retrieveTaskResponse = this.kuflowActivities.retrieveTask(retrieveTaskRequest);

        return retrieveTaskResponse.getTask();
    }

    /**
     * Create task "Notification: No seats available" in KuFlow and wait for its completion
     *
     * @param workflowRequest workflow request
     * @return task created
     */
    private TaskResource createTaskNotificationNoSeatsAvailable(WorkflowRequestResource workflowRequest) {
        UUID taskId = this.kuFlowGenerator.randomUUID();

        CreateTaskRequestResource createTaskRequest = new CreateTaskRequestResource();
        createTaskRequest.setTaskId(taskId);
        createTaskRequest.setTaskDefinitionCode(TASK_CODE_NOTIFICATION_NO_SEATS_AVAILABLE);
        createTaskRequest.setProcessId(workflowRequest.getProcessId());

        // Create and retrieve Task in KuFlow
        this.kuflowActivities.createTaskAndWaitFinished(createTaskRequest);

        RetrieveTaskRequestResource retrieveTaskRequest = new RetrieveTaskRequestResource();
        retrieveTaskRequest.setTaskId(taskId);
        RetrieveTaskResponseResource retrieveTaskResponse = this.kuflowActivities.retrieveTask(retrieveTaskRequest);

        return retrieveTaskResponse.getTask();
    }

    /**
     * Create task "Notification: Reservation complete" in KuFlow and wait for its completion
     *
     * @param workflowRequest workflow request
     * @return task created
     */
    private TaskResource createTaskNotificationReservationComplete(WorkflowRequestResource workflowRequest) {
        UUID taskId = this.kuFlowGenerator.randomUUID();

        CreateTaskRequestResource createTaskRequest = new CreateTaskRequestResource();
        createTaskRequest.setTaskId(taskId);
        createTaskRequest.setTaskDefinitionCode(TASK_CODE_NOTIFICATION_RESERVATION_COMPLETE);
        createTaskRequest.setProcessId(workflowRequest.getProcessId());
        
        //Get the row count to know the seat number and inform user
        String seatNo = this.gSheetsActivities.getSeatNo();
        createTaskRequest.putElementValuesItem("seatNo", TaskElementValueWrapperResource.of(seatNo));
        
        // Create and retrieve Task in KuFlow
        this.kuflowActivities.createTaskAndWaitFinished(createTaskRequest);

        RetrieveTaskRequestResource retrieveTaskRequest = new RetrieveTaskRequestResource();
        retrieveTaskRequest.setTaskId(taskId);
        RetrieveTaskResponseResource retrieveTaskResponse = this.kuflowActivities.retrieveTask(retrieveTaskRequest);

        return retrieveTaskResponse.getTask();
    }
   
}
