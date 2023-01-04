import {
  createAndVisitDashboardWithQuestion,
  createNativeQuestion,
  createStructuredQuestion,
  editDashboard,
  popover,
  restore,
  mapFilterToQuestion,
  saveDashboard,
  setFilter,
  setupCustomList,
  setupNativeQuestionSource,
  setupStructuredQuestionSource,
} from "__support__/e2e/helpers";
import { SAMPLE_DATABASE } from "__support__/e2e/cypress_sample_database";

const { PRODUCTS_ID, PRODUCTS } = SAMPLE_DATABASE;

describe("scenarios > dashboard > filters", () => {
  beforeEach(() => {
    restore();
    cy.signInAsAdmin();
    cy.intercept("POST", "/api/dashboard/**/query").as("getCardQuery");
  });

  it("should be able to use a structured question source", () => {
    createStructuredQuestion(PRODUCTS_ID, PRODUCTS.CATEGORY);
    createAndVisitDashboardWithQuestion(PRODUCTS_ID);

    editDashboard();
    setFilter("Text or Category", "Dropdown");
    mapFilterToQuestion();
    editDropdown();
    setupStructuredQuestionSource();
    saveDashboard();
    filterDashboard();
  });

  it("should be able to use a native question source", () => {
    createNativeQuestion();
    createAndVisitDashboardWithQuestion(PRODUCTS_ID);

    editDashboard();
    setFilter("Text or Category", "Dropdown");
    mapFilterToQuestion();
    editDropdown();
    setupNativeQuestionSource();
    saveDashboard();
    filterDashboard();
  });

  it("should be able to use a static list source", () => {
    createAndVisitDashboardWithQuestion(PRODUCTS_ID);

    editDashboard();
    setFilter("Text or Category", "Dropdown");
    mapFilterToQuestion();
    editDropdown();
    setupCustomList();
    saveDashboard();
    filterDashboard();
  });
});

const editDropdown = () => {
  cy.findByText("Dropdown list").click();
  cy.findByText("Edit").click();
};

const filterDashboard = () => {
  cy.findByText("Text").click();

  popover().within(() => {
    cy.findByText("Doohickey").should("be.visible");
    cy.findByText("Gadget").should("be.visible");
    cy.findByText("Gizmo").should("not.exist");

    cy.findByPlaceholderText("Search the list").type("Gadget");
    cy.findByText("Doohickey").should("not.exist");
    cy.findByText("Gadget").click();
    cy.button("Add filter").click();
    cy.wait("@getCardQuery");
  });
};
