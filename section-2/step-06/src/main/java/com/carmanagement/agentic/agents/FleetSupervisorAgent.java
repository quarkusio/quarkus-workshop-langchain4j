package com.carmanagement.agentic.agents;

import dev.langchain4j.agentic.declarative.SupervisorAgent;
import dev.langchain4j.agentic.declarative.SupervisorRequest;
import dev.langchain4j.service.SystemMessage;

/**
 * Supervisor agent that orchestrates the entire car processing workflow.
 * Coordinates feedback analysis agents and action agents based on car condition.
 * Implements TRUE human-in-the-loop pattern for high-value vehicle dispositions.
 */
public interface FleetSupervisorAgent {

    @SystemMessage("""
        You are a fleet supervisor for a car rental company. You coordinate action agents based on feedback analysis.
        
        The feedback has already been analyzed and you have these inputs:
        - cleaningRequest: What cleaning is needed (or "CLEANING_NOT_REQUIRED")
        - maintenanceRequest: What maintenance is needed (or "MAINTENANCE_NOT_REQUIRED")
        - dispositionRequest: Whether severe damage requires disposition (or "DISPOSITION_NOT_REQUIRED")
        
        ═══════════════════════════════════════════════════════════════════════════
        CRITICAL RULE #1: CHECK dispositionRequest FIRST - DO NOT MAKE YOUR OWN DECISION
        ═══════════════════════════════════════════════════════════════════════════
        
        IF dispositionRequest contains "DISPOSITION_NOT_REQUIRED":
           → The car does NOT need disposition
           → DO NOT invoke PricingAgent
           → DO NOT invoke DispositionProposalAgent
           → DO NOT invoke HumanApprovalAgent
           → DO NOT invoke DispositionAgent
           → ONLY handle cleaning/maintenance if needed
           → Include "APPROVAL_NOT_REQUIRED" in your response
           → STOP - Do not continue to disposition logic
        
        IF dispositionRequest contains "DISPOSITION_REQUIRED":
           → Continue with disposition workflow below
        
        ═══════════════════════════════════════════════════════════════════════════
        DISPOSITION WORKFLOW (ONLY if dispositionRequest = "DISPOSITION_REQUIRED")
        ═══════════════════════════════════════════════════════════════════════════
        
        Step 1: Invoke PricingAgent to estimate car value
        Step 2: Extract the NUMERIC value (e.g., $9,180 = 9180, $20,400 = 20400)
        Step 3: Compare ONLY the numeric value to 15000
        
        IF numeric value > 15000 (HIGH-VALUE - requires human approval):
           a) Invoke DispositionProposalAgent to create proposal
           b) Invoke HumanApprovalAgent (workflow will PAUSE for human decision)
           c) If HumanApprovalAgent returns APPROVED:
              - You MUST include the exact keyword "APPROVED_BY_USER" in your final response
              - Execute the approved disposition action
              - DO NOT invoke any other agents unless the action is KEEP
           d) If HumanApprovalAgent returns REJECTED:
              - You MUST include the exact keyword "REJECTED_BY_USER" in your final response
              - The human wants to keep and repair the vehicle instead
              - Invoke MaintenanceAgent to repair the vehicle
              - DO NOT invoke DispositionAgent
              - DO NOT try to disposition the vehicle in any other way
        
        IF numeric value <= 15000 (LOW-VALUE - no human approval needed):
           a) DO NOT invoke DispositionProposalAgent
           b) DO NOT invoke HumanApprovalAgent
           c) Invoke DispositionAgent DIRECTLY (it will decide: SCRAP/SELL/DONATE/KEEP)
           d) You MUST include the exact keyword "APPROVAL_NOT_REQUIRED" in your final response
           e) If DispositionAgent says KEEP: invoke MaintenanceAgent or CleaningAgent as needed
        
        CRITICAL: The threshold is EXACTLY 15000. Values like 9180, 14999 are LOW-VALUE (<=15000).
        Values like 15001, 16830, 20400 are HIGH-VALUE (>15000).
        
        ═══════════════════════════════════════════════════════════════════════════
        CLEANING/MAINTENANCE (when disposition not required)
        ═══════════════════════════════════════════════════════════════════════════
        
        - If maintenanceRequest ≠ "MAINTENANCE_NOT_REQUIRED": Invoke MaintenanceAgent
        - If cleaningRequest ≠ "CLEANING_NOT_REQUIRED": Invoke CleaningAgent
        - Can invoke both if both needed
        - If both "NOT_REQUIRED": No action needed, car is ready
        
        ═══════════════════════════════════════════════════════════════════════════
        REQUIRED KEYWORDS IN YOUR FINAL RESPONSE
        ═══════════════════════════════════════════════════════════════════════════
        
        Your final response MUST include EXACTLY ONE of these keywords:
        - "APPROVED_BY_USER" - if human approved disposition (high-value car, value > 15000)
        - "REJECTED_BY_USER" - if human rejected disposition (high-value car, value > 15000)
        - "APPROVAL_NOT_REQUIRED" - if no approval needed (low-value car value <= 15000, or no disposition)
        
        CRITICAL: These keywords are case-sensitive and must appear EXACTLY as shown above.
        The system uses these keywords to route the vehicle correctly.
        """)
    @SupervisorAgent(
        outputKey = "supervisorDecision",
        subAgents = {
            PricingAgent.class,
            DispositionProposalAgent.class,
            HumanApprovalAgent.class,
            DispositionAgent.class,
            MaintenanceAgent.class,
            CleaningAgent.class
        }
    )
    String superviseCarProcessing(
        String carMake,
        String carModel,
        Integer carYear,
        Long carNumber,
        String carCondition,
        String rentalFeedback,
        String cleaningFeedback,
        String maintenanceFeedback,
        String cleaningRequest,
        String maintenanceRequest,
        String dispositionRequest
    );

    @SupervisorRequest
    static String request(
        String carMake,
        String carModel,
        Integer carYear,
        Long carNumber,
        String carCondition,
        String cleaningRequest,
        String maintenanceRequest,
        String dispositionRequest,
        String rentalFeedback
    ) {
        // Determine if disposition is required
        boolean dispositionRequired = dispositionRequest != null &&
                                     dispositionRequest.toUpperCase().contains("DISPOSITION_REQUIRED");
        
        if (!dispositionRequired) {
            // No disposition needed - simple path
            return String.format("""
                ═══════════════════════════════════════════════════════════════════════════
                ✅ NO DISPOSITION REQUIRED
                ═══════════════════════════════════════════════════════════════════════════
                
                Car: %d %s %s (#%d)
                Current Condition: %s
                
                Disposition Request: %s
                Cleaning Request: %s
                Maintenance Request: %s
                
                INSTRUCTIONS:
                - DO NOT invoke PricingAgent
                - DO NOT invoke DispositionProposalAgent
                - DO NOT invoke HumanApprovalAgent
                - DO NOT invoke DispositionAgent
                - Only invoke MaintenanceAgent if maintenance needed
                - Only invoke CleaningAgent if cleaning needed
                - Include "APPROVAL_NOT_REQUIRED" in your response
                """,
                carYear, carMake, carModel, carNumber, carCondition,
                dispositionRequest, cleaningRequest, maintenanceRequest
            );
        }
        
        // Disposition required - complex path
        return String.format("""
            ═══════════════════════════════════════════════════════════════════════════
            ⚠️  DISPOSITION REQUIRED - FOLLOW WORKFLOW
            ═══════════════════════════════════════════════════════════════════════════
            
            Car: %d %s %s (#%d)
            Current Condition: %s
            Rental Feedback: %s
            
            Disposition Request: %s
            Cleaning Request: %s
            Maintenance Request: %s
            
            STEP 1: Invoke PricingAgent to get car value
            STEP 2: Extract numeric value for comparison (e.g., "$10,710" → 10710)
            STEP 3: Compare to threshold:
                    - If value > 15000: HIGH-VALUE → Invoke DispositionProposalAgent + HumanApprovalAgent
                    - If value <= 15000: LOW-VALUE → Invoke DispositionAgent directly (NO approval)
            
            IMPORTANT: When invoking DispositionAgent or DispositionProposalAgent:
            - Pass carValue as a STRING with dollar sign (e.g., "$10,710" not 10710)
            - Use the EXACT format from PricingAgent's response
            
            REMEMBER: You MUST include one of these keywords in your final response:
            - "APPROVED_BY_USER" (if human approved)
            - "REJECTED_BY_USER" (if human rejected)
            - "APPROVAL_NOT_REQUIRED" (if value <= 15000, no approval needed)
            """,
            carYear, carMake, carModel, carNumber, carCondition, rentalFeedback,
            dispositionRequest, cleaningRequest, maintenanceRequest
        );
    }
}


