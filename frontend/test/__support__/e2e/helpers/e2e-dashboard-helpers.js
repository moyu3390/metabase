import { visitDashboard } from "__support__/e2e/helpers";
import { modal, popover } from "./e2e-ui-elements-helpers";

// Metabase utility functions for commonly-used patterns
export function selectDashboardFilter(selection, filterName) {
  selection.contains("Select…").click();
  popover().contains(filterName).click({ force: true });
}

export function getDashboardCard(index = 0) {
  return cy.get(".DashCard").eq(index);
}

export function showDashboardCardActions(index = 0) {
  getDashboardCard(index).realHover();
}

export function editDashboard() {
  cy.icon("pencil").click();
}

export function saveDashboard() {
  cy.findByText("Save").click();
  cy.findByText("You're editing this dashboard.").should("not.exist");
}

export function checkFilterLabelAndValue(label, value) {
  cy.get("fieldset").find("legend").invoke("text").should("eq", label);

  cy.get("fieldset").contains(value);
}

export function setFilter(type, subType) {
  cy.icon("filter").click();

  cy.findByText("What do you want to filter?");

  popover().within(() => {
    cy.findByText(type).click();

    if (subType) {
      cy.findByText(subType).click();
    }
  });
}

export function addTextBox(string, options = {}) {
  cy.icon("pencil").click();
  cy.icon("string").click();
  cy.findByPlaceholderText(
    "You can use Markdown here, and include variables {{like_this}}",
  ).type(string, options);
}

export function createStructuredQuestion(products_table_id, product_category) {
  const structuredQuestionDetails = {
    name: "GUI categories",
    query: {
      "source-table": products_table_id,
      aggregation: [["count"]],
      breakout: [["field", product_category, null]],
      filter: ["!=", ["field", product_category, null], "Gizmo"],
    },
  };
  cy.createQuestion(structuredQuestionDetails, {
    wrapId: true,
    idAlias: "structuredQuestionId",
  });
}

export function createAndVisitDashboardWithQuestion(products_table_id) {
  const dashboardQuestionDetails = {
    display: "scalar",
    query: {
      "source-table": products_table_id,
      aggregation: [["count"]],
    },
  };
  cy.createQuestionAndDashboard({
    questionDetails: dashboardQuestionDetails,
  }).then(({ body: { dashboard_id } }) => {
    visitDashboard(dashboard_id);
  });
}

export function createNativeQuestion() {
  const nativeQuestionDetails = {
    name: "SQL categories",
    native: {
      query: "select distinct CATEGORY from PRODUCTS order by CATEGORY limit 2",
    },
  };
  cy.createNativeQuestion(nativeQuestionDetails);
}

export function setupStructuredQuestionSource() {
  cy.findByText("Values from a model or question").click();
  modal().within(() => {
    cy.findByText("Saved Questions").click();
    cy.findByText("GUI categories").click();
    cy.button("Select column").click();
  });
  modal().within(() => {
    cy.findByText("Pick a column").click();
  });
  popover().within(() => {
    cy.findByText("Category").click();
  });
  modal().within(() => {
    cy.button("Done").click();
  });
}

export function setupNativeQuestionSource() {
  cy.findByText("Values from a model or question").click();
  modal().within(() => {
    cy.findByText("Saved Questions").click();
    cy.findByText("SQL categories").click();
    cy.button("Select column").click();
  });
  modal().within(() => {
    cy.findByText("Pick a column").click();
  });
  popover().within(() => {
    cy.findByText("CATEGORY").click();
  });
  modal().within(() => {
    cy.button("Done").click();
  });
}

export function setupCustomList() {
  cy.findByText("Custom list").click();
  modal().within(() => {
    cy.findByPlaceholderText(/banana/).type("Doohickey\nGadget");
    cy.button("Done").click();
  });
}

export function mapFilterToQuestion() {
  cy.findByText("Select…").click();
  popover().within(() => cy.findByText("Category").click());
}
