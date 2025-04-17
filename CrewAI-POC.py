import os
from crewai import Agent, Task
from langgraph.graph import StateGraph, END
from langchain_openai import ChatOpenAI
from crewai_tools import tool

# Set up OpenAI LLM
os.environ["OPENAI_API_KEY"] = "your-api-key"
llm = ChatOpenAI(model="gpt-3.5-turbo")

# Define Tools
@tool("Confluence Crawler Tool")
def confluence_crawler(url: str) -> str:
    """Crawl a Confluence page and return its content."""
    # Add actual Confluence API implementation here
    return f"Mock content from Confluence page: {url}"

@tool("Splunk Log Fetcher Tool")
def splunk_log_fetcher(query: str) -> str:
    """Fetch logs from Splunk based on a search query."""
    # Add actual Splunk API implementation here
    return f"Mock logs for query: {query}"

@tool("Email Sender Tool")
def email_sender(recipient: str, content: str) -> str:
    """Send an email with the specified content."""
    # Add actual email sending implementation here
    return f"Mock email sent to {recipient} with content: {content}"

# Create Agents
confluence_agent = Agent(
    role='Confluence Specialist',
    goal='Extract relevant information from Confluence pages',
    backstory='Experienced in knowledge management and Confluence navigation',
    tools=[confluence_crawler],
    llm=llm,
    verbose=True
)

decision_agent = Agent(
    role='Security Analyst',
    goal='Determine required actions based on Confluence content',
    backstory='Expert in security protocols and incident response',
    llm=llm,
    verbose=True
)

splunk_agent = Agent(
    role='Splunk Investigator',
    goal='Retrieve relevant security logs from Splunk',
    backstory='Skilled in Splunk querying and log analysis',
    tools=[splunk_log_fetcher],
    llm=llm,
    verbose=True
)

email_agent = Agent(
    role='Communications Specialist',
    goal='Send clear and concise security notifications',
    backstory='Experienced in technical communications and alerting',
    tools=[email_sender],
    llm=llm,
    verbose=True
)

# Define Workflow State
workflow_state = {
    "confluence_url": "https://confluence.example.com/security-policy",
    "content": None,
    "decision": None,
    "output": None
}

# Define Node Functions
def crawl_confluence(state):
    task = Task(
        description=f"Crawl Confluence page at {state['confluence_url']}",
        agent=confluence_agent
    )
    state["content"] = task.execute()
    return state

def make_decision(state):
    task = Task(
        description=f"""Analyze this Confluence content and decide if Splunk investigation 
        is needed or if we should send an email notification. Content: {state['content']}""",
        agent=decision_agent,
        expected_output="Either 'splunk' or 'email'"
    )
    decision = task.execute().lower()
    state["decision"] = "splunk" if "splunk" in decision else "email"
    return state

def investigate_splunk(state):
    task = Task(
        description="Search Splunk for relevant security logs",
        agent=splunk_agent,
        expected_output="Security logs from Splunk"
    )
    state["output"] = task.execute()
    return state

def send_email(state):
    task = Task(
        description="Send security notification email to SOC team",
        agent=email_agent,
        expected_output="Confirmation of sent email"
    )
    state["output"] = task.execute()
    return state

# Create LangGraph Workflow
workflow = StateGraph(initial_state=workflow_state)

# Add Nodes
workflow.add_node("crawl_confluence", crawl_confluence)
workflow.add_node("make_decision", make_decision)
workflow.add_node("investigate_splunk", investigate_splunk)
workflow.add_node("send_email", send_email)

# Set Up Transitions
workflow.add_edge("crawl_confluence", "make_decision")

# Conditional Edge
def route_decision(state):
    if state["decision"] == "splunk":
        return "investigate_splunk"
    return "send_email"

workflow.add_conditional_edges(
    "make_decision",
    route_decision,
    {
        "investigate_splunk": "investigate_splunk",
        "send_email": "send_email"
    }
)

# Final Transitions
workflow.add_edge("investigate_splunk", END)
workflow.add_edge("send_email", END)

# Compile and Run
app = workflow.compile()
result = app.invoke(workflow_state)

print("\nFinal Result:")
print(result["output"])


  In mobile banking systems, manually entering beneficiary details such as account numbers, IFSC codes, and names can be error-prone and time-consuming. Such manual processes are also vulnerable to fraudulent manipulation. Existing solutions require secure internet-based APIs or QR scanning, but these depend heavily on the network, device camera quality, and complex back-end integrations.

There is a need for a simple, low-bandwidth, secure, and device-agnostic mechanism to transmit beneficiary information that ensures accuracy, ease of use, and improved user experience. DTMF tones, traditionally used in telecommunication, present a unique opportunity for encoding and transferring structured information via sound signals in an offline or near-offline mode.


4. Summary of the Invention
This invention proposes a novel method of adding a beneficiary in a bank account using DTMF tones. The account details of the beneficiary (including account number, IFSC code, and optionally nickname) are encoded into a unique numeric sequence which is then converted to a series of DTMF tones. These tones are transmitted to the customer’s device (e.g., during a call or via speaker from a physical terminal). The customer’s mobile banking app, equipped with a decoding module, listens for these tones, decodes them back into structured data, and prepopulates the beneficiary form with the extracted details.

This method enhances user experience, reduces errors, improves security by reducing manual data entry, and enables contactless and low-bandwidth beneficiary addition.


    . Brief Description of the Drawings
Figure 1: System architecture showing the flow from beneficiary data input to DTMF tone generation and decoding.

Figure 2: Flowchart of the encoding and DTMF tone generation process.

Figure 3: Flowchart of the DTMF tone reception and decoding by the mobile app.

Figure 4: Screenshot of the mobile banking interface with prepopulated beneficiary data.


Patent Application: Peer-to-Peer Beneficiary Addition Using DTMF Tone-Based Secure Exchange
1. Title of the Invention
System and Method for Peer-to-Peer Bank Beneficiary Addition Using DTMF-Based Secure Audio Transmission Between Mobile Applications

2. Field of the Invention
The present invention relates to digital banking systems and secure peer-to-peer data exchange. More specifically, it pertains to a system and method enabling one bank customer to share their account details securely with another customer by encoding the details into DTMF (Dual-Tone Multi-Frequency) tones transmitted via audio from one mobile banking application and decoded by another, to prepopulate beneficiary information.

3. Background of the Invention
Adding a beneficiary in mobile banking typically involves manually entering details such as account number and IFSC code, which is error-prone and inefficient. While QR code scanning and contact-based sharing are alternatives, they rely on camera quality, proximity, or internet connectivity, which may not always be available or user-friendly.

There is a need for a contactless, secure, and simple mechanism for transferring account details between two individuals using the same banking platform. Leveraging DTMF audio signals as a means of encoding and transferring data offers a robust solution that works across devices without requiring network connectivity, QR code scanning, or complex integrations.

4. Summary of the Invention
The invention enables secure, peer-to-peer beneficiary addition using DTMF audio tones. When Person2 (User B) wants to add Person1 (User A) as a beneficiary, both users launch the same bank’s mobile application.

Person1 initiates the process by triggering a DTMF tone from their app, which encodes their own account details (account number, IFSC code, and a one-time session ID). Person2’s mobile app listens to this audio signal via the microphone, decodes it in real time, and extracts the account information. These details are automatically populated in the "Add Beneficiary" form on Person2’s app, subject to user verification and optional authentication.

This invention enhances convenience, improves data accuracy, and ensures a secure, offline-compatible method of beneficiary onboarding.

5. Brief Description of the Drawings
Figure 1: System overview of the peer-to-peer DTMF-based beneficiary addition.

Figure 2: Sequence diagram showing interaction between Person1’s and Person2’s apps.

Figure 3: Flowchart of DTMF encoding and generation.

Figure 4: Flowchart of DTMF audio reception and decoding.

Figure 5: Screenshot of mobile banking interface showing auto-filled beneficiary details.

6. Detailed Description of the Invention
Participants:
Person1 (User A): The beneficiary whose details are to be shared.

Person2 (User B): The user who wants to add Person1 as a beneficiary.

Components:
Mobile Banking Application (common to both users)

DTMF Encoder Module

DTMF Audio Generator

DTMF Listener/Decoder Module

Form Prepopulation Engine

Session Validation Mechanism

Process:
Initiation by Person1:

Person1 logs into the mobile banking app and navigates to “Share My Account Details.”

On tap, the app collects Person1’s account number, IFSC code, and generates a time-bound session ID.

These data points are serialized, encrypted, and encoded into a numeric string.

The string is converted into a sequence of DTMF tones.

The app plays this DTMF audio through the phone speaker.

Reception by Person2:

Person2 logs into the banking app and opens the “Add Beneficiary via Audio” feature.

The app activates the microphone to listen for the DTMF tones.

On detecting the tone sequence, it:

Converts the audio signal to numeric digits via a DTMF decoder.

Validates the data using the session ID and checksum.

Decrypts the payload to retrieve account number and IFSC.

Prepopulation and Confirmation:

The app automatically fills the "Add Beneficiary" form with the decoded account number and IFSC code.

Person2 can verify and submit the form to complete beneficiary addition.

Key Features:
Offline/low-bandwidth functionality: No internet is needed during the data exchange.

Security through encryption, session ID, and timeouts.

No physical contact or QR scanning required.

Device-agnostic implementation – works with any smartphone that has a microphone and speaker.

7. Claims
A method for adding a beneficiary to a mobile banking application comprising:

triggering a DTMF tone from a first user’s banking application, the tone representing encrypted account details and session metadata;

capturing said tone through a second user’s mobile banking application;

decoding the tone to extract the beneficiary details; and

prepopulating the "Add Beneficiary" form with the decoded information.

The method of claim 1, wherein the encoded tone includes a session identifier and is valid only within a predefined time period.

The method of claim 1, wherein the DTMF tone is generated from serialized data comprising account number, IFSC code, and session ID.

The method of claim 1, wherein the decoding process includes decryption and checksum validation.

The method of claim 1, wherein the mobile banking app confirms the beneficiary addition using user authentication such as biometrics or PIN.

A system comprising:

a DTMF encoder within a mobile banking application configured to convert user account data into DTMF tones;

a DTMF decoder in another instance of the mobile banking application configured to receive and decode said tones;

a data verification module;

a form prepopulation engine that auto-fills beneficiary fields based on decoded data.

8. Abstract
A system and method are disclosed for adding a bank account beneficiary using secure peer-to-peer DTMF audio transmission between mobile applications. When two customers of the same bank wish to perform a beneficiary addition, the beneficiary initiates the process by generating a DTMF-encoded audio signal containing their account number, IFSC code, and session ID. The recipient captures this tone using the mobile banking application’s built-in decoder, extracts the relevant information, and sees the beneficiary fields automatically filled in their interface. This improves usability, eliminates manual entry, and enables secure offline data exchange.

9. Drawings (Description Only)
Figure 1: End-to-end architecture showing both users’ mobile apps interacting over an audio channel.

Figure 2: Sequence of operations between sender (Person1) and receiver (Person2).

Figure 3: Flowchart of encoding and tone generation from account data.

Figure 4: Flowchart of DTMF decoding and prepopulation process.

Figure 5: Mobile app screen with auto-filled beneficiary information.



