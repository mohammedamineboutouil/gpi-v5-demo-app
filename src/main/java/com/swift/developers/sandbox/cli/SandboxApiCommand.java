package com.swift.developers.sandbox.cli;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.swift.commons.context.Context;
import com.swift.commons.context.KeyStoreContext;
import com.swift.commons.exceptions.NRSignatureException;
import com.swift.commons.exceptions.SignatureContextException;
import com.swift.commons.token.ChannelToken;
import com.swift.commons.token.Token;
import com.swift.sdk.common.entity.ConnectionInfo;
import com.swift.sdk.common.entity.Constants;
import com.swift.sdk.common.entity.SecurityFootprintType;
import com.swift.sdk.gpi.preval.v231.api.PaymentPreValidationApi;
import com.swift.sdk.gpi.preval.v231.model.AccountVerificationRequest;
import com.swift.sdk.gpi.preval.v231.model.AmountValidation1;
import com.swift.sdk.gpi.tracker.v5.status.update.cct.model.PaymentStatusRequest2;
import com.swift.sdk.gpi.tracker.v5.status.update.cov.model.PaymentScenario7Code;
import com.swift.sdk.gpi.tracker.v5.status.update.cov.model.PaymentStatusRequest3;
import com.swift.sdk.gpi.tracker.v5.status.update.fit.model.BusinessService18Code;
import com.swift.sdk.gpi.tracker.v5.status.update.fit.model.PaymentScenario8Code;
import com.swift.sdk.gpi.tracker.v5.status.update.fit.model.PaymentStatusRequest7;
import com.swift.sdk.gpi.tracker.v5.status.update.inst.model.BusinessService16Code;
import com.swift.sdk.gpi.tracker.v5.status.update.inst.model.PaymentStatusRequest5;
import com.swift.sdk.gpi.tracker.v5.status.update.universal.model.PaymentStatusRequest4;
import com.swift.sdk.gpi.tracker.v5.status.update.universal.model.TransactionIndividualStatus5Code;
import com.swift.sdk.gpi.tracker.v5.transactionsandcancellations.ApiException;
import com.swift.sdk.gpi.tracker.v5.transactionsandcancellations.api.CancelTransactionApi;
import com.swift.sdk.gpi.tracker.v5.transactionsandcancellations.api.GetChangedPaymentTransactionsApi;
import com.swift.sdk.gpi.tracker.v5.transactionsandcancellations.api.GetPaymentTransactionDetailsApi;
import com.swift.sdk.gpi.tracker.v5.transactionsandcancellations.api.TransactionCancellationStatusApi;
import com.swift.sdk.gpi.tracker.v5.transactionsandcancellations.model.*;
import com.swift.sdk.management.handler.config.SDKYamlConfigLoader;
import com.swift.sdk.management.util.SwiftApiService;
import com.swift.sdk.management.util.Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.Callable;

@Slf4j
@Component
@RequiredArgsConstructor
@Command(
        name = "gpi v5 APIs",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = """
                A simple Java client application consuming gpi v5 APIs using SWIFT SDK.
                """
)
public class SandboxApiCommand implements Callable<Integer> {

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    @Value("${swift.connect.config:}")
    private Resource encConfigFile;

    @Value("${swift.connect.secret:}")
    private Resource encSecretFile;
    private ConnectionInfo connectionInfo;

    @PostConstruct
    public void initConfig() throws IOException {
        final var encConfigFilePath = encConfigFile.getFile().getAbsolutePath();
        final var encSecretFilePath = encSecretFile.getFile().getAbsolutePath();

        System.out.println("Using the configuration file - " + encConfigFilePath + " to setup the session.");

        //### INDIRECT MODE :: SDK via MGW  ###
        //Constants.isNewAbsPath=true;  // absPath compatibility

        //### DIRECT MODE :: SDK to Api Gateway  ###
        //Constants.securityFootprint = SecurityFootprintType.HARD;
        Constants.securityFootprint = SecurityFootprintType.SOFT;
        //Constants.securityFootprint = SecurityFootprintType.CLOUD;

        //### ENC CONFIG FILE :: Encrypted config file  ###
        Constants.ENC_CONFIG_FILEPATH = encConfigFilePath;
        //### ENC SECRET KEY :: Enc secrets key  ###
        Constants.ENC_SECRETS_FILEPATH = encSecretFilePath;

        this.connectionInfo = SDKYamlConfigLoader.loadYaml();

        System.out.println("\nConfiguration is loaded successfully.");
    }

    @Override
    public Integer call() {
        try (Scanner scan = new Scanner(System.in)) {
            String userInput;

            do {
                displayApiSelectionMenu();
                userInput = scan.nextLine().trim();

                if (!userInput.isEmpty() && isNumeric(userInput)) {
                    int apiChoice = Integer.parseInt(userInput);
                    callSelectedApi(apiChoice);
                }

            } while (!userInput.equalsIgnoreCase("bye"));
        }

        // Return an exit code (0 for success, non-zero for errors)
        return 0;
    }

    private void displayApiSelectionMenu() {
        System.out.print("""
                \n--------------Select the GPI API you would like to call-------------------
                1 - CCT Status Confirmation
                2 - COV Status Confirmation
                3 - FIT Status Confirmation
                4 - INST Status Confirmation
                5 - Universal Status Confirmation
                6 - Get Payment Transaction Details
                7 - Get Changed Payment Transaction
                8 - Cancel Transaction
                9 - Transaction Cancellation Status
                \n--------------Select the (Pre-Validation APIs) you would like to call-------------------
                10 - Verify Beneficiary Account
                11 - Amount Validation Status

                Select an API you would like to call or 'bye' to exit:\s
                """);
    }

    private boolean isNumeric(String input) {
        return input.chars().allMatch(Character::isDigit);
    }

    private void callSelectedApi(int apiChoice) {
        switch (apiChoice) {
            case 1 -> statusConfirmationCCT();
            case 2 -> statusConfirmationCOV();
            case 3 -> statusConfirmationFIT();
            case 4 -> statusConfirmationINST();
            case 5 -> statusConfirmationUni();
            case 6 -> getPaymentTransactionDetails();
            case 7 -> getChangedPaymentTransaction();
            case 8 -> cancelTransaction();
            case 9 -> transactionCancellationStatus();
            case 10 -> verifyBeneficiaryAccount();
            case 11 -> amountValidationStatus();
            default -> System.out.println("Invalid option. Please try again.");
        }
    }

    private void amountValidationStatus() {
        final var api = new PaymentPreValidationApi(connectionInfo);
        final var basePath = Util.getBasePath(SwiftApiService.preval_service_v2);
        api.setBasePath(basePath);

        final var body = """
                {
                  "amount": "6545",
                  "currency_code": "XOF"
                }
                """;

        final var reqBody = GSON.fromJson(body, AmountValidation1.class);
        final var xBic = "swhqbebb";

        try {
            final var response = api.getAmountValidationStatus(xBic, reqBody, UUID.randomUUID());

            final var jsonOutput = GSON.toJson(response);

            System.out.println("\nUrl:\n" + api.computeAudienceGetAmountValidationStatus() + "\nX-SWIFT-Signature:\n" + null + "\nRequest Body:\n" + GSON.toJson(reqBody) + "\nResponse:\n" + jsonOutput);
        } catch (com.swift.sdk.gpi.preval.v231.ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void verifyBeneficiaryAccount() {
        final var api = new PaymentPreValidationApi(connectionInfo);
        final var basePath = Util.getBasePath(SwiftApiService.preval_service_v2);
        api.setBasePath(basePath);

        final var body = """
                {
                  "correlation_identifier": "string",
                  "context": "BENR",
                  "uetr": "dd3027ea-9460-480c-8ef4-0aed819a5ce8",
                  "creditor_account": "string",
                  "creditor_name": "string",
                  "creditor_address": {
                    "department": "string",
                    "sub_department": "string",
                    "street_name": "string",
                    "building_number": "string",
                    "building_name": "string",
                    "floor": "string",
                    "post_box": "string",
                    "room": "string",
                    "post_code": "string",
                    "town_name": "string",
                    "town_location_name": "string",
                    "district_name": "string",
                    "country_sub_division": "string",
                    "country": "YJ",
                    "address_line": [
                      "string"
                    ]
                  },
                  "creditor_organisation_identification": {
                    "any_bic": "RZIPZHCQ",
                    "other": [
                      {
                        "identification": "string",
                        "issuer": "string"
                      }
                    ]
                  },
                  "creditor_agent": {
                    "bicfi": "71Q8QA7QYEB",
                    "clearing_system_member_identification": {
                      "member_identification": "string"
                    }
                  },
                  "creditor_agent_branch_identification": "string"
                }
                """;

        final var reqBody = GSON.fromJson(body, AccountVerificationRequest.class);
        final var xBic = "cclabebb";

        try {
            final var response = api.verifyBeneficiaryAccountWithHttpInfo(xBic, reqBody, UUID.randomUUID(), OffsetDateTime.now());

            final var jsonOutput = GSON.toJson(response);

            System.out.println("\nUrl:\n" + api.computeAudienceVerifyBeneficiaryAccount() + "\nX-SWIFT-Signature:\n" + null + "\nRequest Body:\n" + GSON.toJson(reqBody) + "\nResponse:\n" + jsonOutput);
        } catch (com.swift.sdk.gpi.preval.v231.ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void transactionCancellationStatus() {
        final var api = new TransactionCancellationStatusApi(connectionInfo);
        final var basePath = Util.getBasePath(SwiftApiService.tracker_service_v5);
        api.setBasePath(basePath);

        final var reqBody = new TransactionCancellationStatusRequest2();

        reqBody.setFrom("BANBUS33XXX");
        reqBody.setServiceLevel(BusinessService11Code.G002);
        reqBody.setCaseIdentification("123");
        reqBody.setInvestigationExecutionStatus(InvestigationExecutionConfirmation5Code.CNCL);
        reqBody.setAssignmentIdentification("resolvedcase123");

        byte[] signature = createSignature(basePath, reqBody.toString());

        final var uetr = "97ed4827-7b6f-4491-a06f-b548d5a7512d";

        try {
            final var response = api.transactionCancellationStatusWithHttpInfo(reqBody, signature, uetr);

            final var jsonOutput = GSON.toJson(response);

            System.out.println("\nUrl:\n" + api.computeAudienceTransactionCancellationStatus(uetr) + "\nX-SWIFT-Signature:\n" + new String(signature, StandardCharsets.UTF_8) + "\nRequest Body:\n" + GSON.toJson(reqBody) + "\nResponse:\n" + jsonOutput);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void cancelTransaction() {
        final var api = new CancelTransactionApi(connectionInfo);
        final var basePath = Util.getBasePath(SwiftApiService.tracker_service_v5);
        api.setBasePath(basePath);

        final var reqBody = new CancelTransactionRequest2();

        reqBody.setFrom("BANABEBBXXX");
        reqBody.setServiceLevel(BusinessService11Code.G002);
        reqBody.setCaseIdentification("123");
        reqBody.setOriginalInstructionIdentification("XYZ");
        reqBody.setCancellationReasonInformation(CancellationReason8Code.DUPL);

        byte[] signature = createSignature(basePath, reqBody.toString());

        final var uetr = "97ed4827-7b6f-4491-a06f-b548d5a7512d";

        try {
            final var response = api.cancelTransactionWithHttpInfo(reqBody, signature, uetr);

            final var jsonOutput = GSON.toJson(response);

            System.out.println("\nUrl:\n" + api.computeAudienceCancelTransaction(uetr) + "\nX-SWIFT-Signature:\n" + new String(signature, StandardCharsets.UTF_8) + "\nRequest Body:\n" + GSON.toJson(reqBody) + "\nResponse:\n" + jsonOutput);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void getChangedPaymentTransaction() {
        final var api = new GetChangedPaymentTransactionsApi(connectionInfo);
        final var basePath = Util.getBasePath(SwiftApiService.tracker_service_v5);
        api.setBasePath(basePath);

        OffsetDateTime fromDateTime = OffsetDateTime.parse("2020-04-11T00:00:00.0Z");
        OffsetDateTime toDateTime = OffsetDateTime.parse("2020-04-16T00:00:00.0Z");
        int maxNumber = Integer.parseInt("10");
        String paymentScenario = "CCTR";
        String next = null;

        try {
            final var response = api.getChangedPaymentTransactions(fromDateTime, toDateTime, maxNumber, paymentScenario, next);

            final var jsonOutput = GSON.toJson(response);

            System.out.println("\nUrl:\n" + api.computeAudienceGetChangedPaymentTransactions() + "\nX-SWIFT-Signature:\n" + null + "\nRequest Body:\n" + null + "\nResponse:\n" + jsonOutput);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void getPaymentTransactionDetails() {
        final var api = new GetPaymentTransactionDetailsApi(connectionInfo);
        final var basePath = Util.getBasePath(SwiftApiService.tracker_service_v5);
        api.setBasePath(basePath);

        final var uetr = "97ed4827-7b6f-4491-a06f-b548d5a7512d";

        try {
            final var response = api.getPaymentTransactionDetails(uetr);

            final var jsonOutput = GSON.toJson(response);

            System.out.println("\nUrl:\n" + api.computeAudienceGetPaymentTransactionDetails(uetr) + "\nX-SWIFT-Signature:\n" + null + "\nRequest Body:\n" + null + "\nResponse:\n" + jsonOutput);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void statusConfirmationUni() {
        final var api = new com.swift.sdk.gpi.tracker.v5.status.update.universal.api.StatusConfirmationsApi(connectionInfo);
        final var basePath = Util.getBasePath(SwiftApiService.tracker_service_v5_status_confirm_universal);
        api.setBasePath(basePath);

        final var reqBody = new PaymentStatusRequest4();

        reqBody.setFrom("BANCUS33XXX");
        reqBody.setInstructionIdentification("jkl000");
        reqBody.setTransactionStatus(TransactionIndividualStatus5Code.ACCC);
        reqBody.setTrackerInformingParty("BANABEBBXXX");

        final var uetr = "54ed4827-7b6f-4491-a06f-b548d5a7512d";

        try {
            final var response = api.statusConfirmationsWithHttpInfo(uetr, reqBody);

            final var jsonOutput = GSON.toJson(response);

            System.out.println("\nUrl:\n" + api.computeAudienceStatusConfirmations(uetr) + "\nX-SWIFT-Signature:\n" + null + "\nRequest Body:\n" + GSON.toJson(reqBody) + "\nResponse:\n" + jsonOutput);
        } catch (com.swift.sdk.gpi.tracker.v5.status.update.universal.ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void statusConfirmationINST() {
        final var api = new com.swift.sdk.gpi.tracker.v5.status.update.inst.api.StatusConfirmationsApi(connectionInfo);
        final var basePath = Util.getBasePath(SwiftApiService.tracker_service_v5_status_confirm_inst);
        api.setBasePath(basePath);

        final var reqBody = new PaymentStatusRequest5();

        reqBody.setFrom("BANCUS33XXX");
        reqBody.setInstructionIdentification("jkl000");
        reqBody.setTransactionStatus(com.swift.sdk.gpi.tracker.v5.status.update.inst.model.TransactionIndividualStatus5Code.ACCC);
        reqBody.setTrackerInformingParty("BANABEBBXXX");
        reqBody.setServiceLevel(BusinessService16Code.G005);
        reqBody.setPaymentScenario(com.swift.sdk.gpi.tracker.v5.status.update.inst.model.PaymentScenario6Code.CCTR);
        final var uetr = "54ed4827-7b6f-4491-a06f-b548d5a7512d";

        try {
            final var response = api.statusConfirmationsWithHttpInfo(uetr, reqBody);

            final var jsonOutput = GSON.toJson(response);

            System.out.println("\nUrl:\n" + api.computeAudienceStatusConfirmations(uetr) + "\nX-SWIFT-Signature:\n" + null + "\nRequest Body:\n" + GSON.toJson(reqBody) + "\nResponse:\n" + jsonOutput);
        } catch (com.swift.sdk.gpi.tracker.v5.status.update.inst.ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void statusConfirmationFIT() {
        final var api = new com.swift.sdk.gpi.tracker.v5.status.update.fit.api.StatusConfirmationsApi(connectionInfo);
        final var basePath = Util.getBasePath(SwiftApiService.tracker_service_v5_status_confirm_fit);
        api.setBasePath(basePath);

        final var reqBody = new PaymentStatusRequest7();

        reqBody.setFrom("BANCUS33XXX");
        reqBody.setInstructionIdentification("jkl000");
        reqBody.setTransactionStatus(com.swift.sdk.gpi.tracker.v5.status.update.fit.model.TransactionIndividualStatus5Code.ACCC);
        reqBody.setTrackerInformingParty("BANABEBBXXX");
        reqBody.setServiceLevel(BusinessService18Code.G004);
        reqBody.setPaymentScenario(PaymentScenario8Code.FCTR);
        reqBody.setEndToEndIdentification("123Ref");

        final var uetr = "97ed4827-7b6f-4491-a06f-b548d5a7512d";

        try {
            final var response = api.statusConfirmationsWithHttpInfo(uetr, reqBody);

            final var jsonOutput = GSON.toJson(response);

            System.out.println("\nUrl:\n" + api.computeAudienceStatusConfirmations(uetr) + "\nX-SWIFT-Signature:\n" + null + "\nRequest Body:\n" + GSON.toJson(reqBody) + "\nResponse:\n" + jsonOutput);
        } catch (com.swift.sdk.gpi.tracker.v5.status.update.fit.ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void statusConfirmationCOV() {
        final var api = new com.swift.sdk.gpi.tracker.v5.status.update.cov.api.StatusConfirmationsApi(connectionInfo);
        final var basePath = Util.getBasePath(SwiftApiService.tracker_service_v5_status_confirm_cov);
        api.setBasePath(basePath);

        final var reqBody = new PaymentStatusRequest3();

        reqBody.setFrom("BANCUS33XXX");
        reqBody.setInstructionIdentification("jkl000");
        reqBody.setTransactionStatus(com.swift.sdk.gpi.tracker.v5.status.update.cov.model.TransactionIndividualStatus5Code.ACCC);
        reqBody.setTrackerInformingParty("BANABEBBXXX");
        reqBody.setServiceLevel(com.swift.sdk.gpi.tracker.v5.status.update.cov.model.BusinessService12Code.G001);
        reqBody.setPaymentScenario(PaymentScenario7Code.COVE);
        reqBody.setEndToEndIdentification("123Ref");

        final var uetr = "54ed4827-7b6f-4491-a06f-b548d5a7512d";

        try {
            final var response = api.statusConfirmationsWithHttpInfo(uetr, reqBody);

            final var jsonOutput = GSON.toJson(response);

            System.out.println("\nUrl:\n" + api.computeAudienceStatusConfirmations(uetr) + "\nX-SWIFT-Signature:\n" + null + "\nRequest Body:\n" + GSON.toJson(reqBody) + "\nResponse:\n" + jsonOutput);
        } catch (com.swift.sdk.gpi.tracker.v5.status.update.cov.ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void statusConfirmationCCT() {
        final var api = new com.swift.sdk.gpi.tracker.v5.status.update.cct.api.StatusConfirmationsApi(connectionInfo);
        final var basePath = Util.getBasePath(SwiftApiService.tracker_service_v5_status_confirm_cct);
        api.setBasePath(basePath);

        final var reqBody = new PaymentStatusRequest2();

        reqBody.setFrom("BANCUS33XXX");
        reqBody.setInstructionIdentification("jkl000");
        reqBody.setTransactionStatus(com.swift.sdk.gpi.tracker.v5.status.update.cct.model.TransactionIndividualStatus5Code.ACCC);
        reqBody.setTrackerInformingParty("BANABEBBXXX");
        reqBody.setServiceLevel(com.swift.sdk.gpi.tracker.v5.status.update.cct.model.BusinessService12Code.G001);
        reqBody.setPaymentScenario(com.swift.sdk.gpi.tracker.v5.status.update.cct.model.PaymentScenario6Code.CCTR);

        final var uetr = "46ed4827-7b6f-4491-a06f-b548d5a7512d";

        try {
            final var response = api.statusConfirmationsWithHttpInfo(uetr, reqBody);

            final var jsonOutput = GSON.toJson(response);

            System.out.println("\nUrl:\n" + api.computeAudienceStatusConfirmations(uetr) + "\nX-SWIFT-Signature:\n" + null + "\nRequest Body:\n" + GSON.toJson(reqBody) + "\nResponse:\n" + jsonOutput);
        } catch (com.swift.sdk.gpi.tracker.v5.status.update.cct.ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] createSignature(String aud, String requestBody) {
        final var claims = new HashMap<String, String>();
        claims.put("audience", aud);

        final var json = GSON.toJson(requestBody);
        claims.put("payload", json);

        try {
            Token token = new ChannelToken();
            Context context = new KeyStoreContext(connectionInfo.getCertPath(), connectionInfo.getCertPassword(),
                    connectionInfo.getCertPassword(), connectionInfo.getCertAlias());
            String nrSignature = token.createNRSignature(context, claims);
            return nrSignature.getBytes();
        } catch (SignatureContextException | NRSignatureException e) {
            throw new RuntimeException(e);
        }
    }
}