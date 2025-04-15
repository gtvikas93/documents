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
