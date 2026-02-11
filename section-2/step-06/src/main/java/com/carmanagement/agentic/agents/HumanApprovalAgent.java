package com.carmanagement.agentic.agents;

import com.carmanagement.agentic.tools.HumanApprovalTool;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.ToolBox;

/**
 * TRUE Human-in-the-Loop Agent that uses a tool to pause workflow execution
 * and wait for actual human approval through the UI.
 *
 * This agent has access to the requestHumanApproval tool which BLOCKS execution
 * until a human makes a decision via the REST API and UI.
 */
public interface HumanApprovalAgent {

    @SystemMessage("""
        You are a human approval coordinator for high-value vehicle dispositions.
        
        Your role is to request human approval for disposition proposals by using the requestHumanApproval tool.
        
        IMPORTANT: You MUST call the requestHumanApproval tool with ALL the provided information.
        The tool will pause the workflow and wait for a human to approve or reject the proposal through the UI.
        
        After calling the tool, you will receive the human's decision. Format your response as:
        
        Decision: [APPROVED or REJECTED]
        Reason: [The human's reasoning]
        
        Do not make decisions yourself - always use the tool to get human input.
        """)
    @UserMessage("""
        A disposition proposal needs human approval:
        
        Vehicle: {carYear} {carMake} {carModel} (#{carNumber})
        Estimated Value: {carValue}
        Current Condition: {carCondition}
        Damage Report: {rentalFeedback}
        
        Proposed Action: {proposedDisposition}
        Reasoning: {dispositionReason}
        
        Use the requestHumanApproval tool to get human approval for this proposal.
        Pass ALL the information to the tool.
        """)
    @Agent(outputKey = "approvalDecision", description = "Coordinates human approval for high-value vehicle dispositions using the requestHumanApproval tool")
    @ToolBox(HumanApprovalTool.class)
    String reviewDispositionProposal(
            String carMake,
            String carModel,
            Integer carYear,
            Integer carNumber,
            String carValue,
            String proposedDisposition,
            String dispositionReason,
            String carCondition,
            String rentalFeedback);
}
