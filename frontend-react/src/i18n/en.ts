import type { TranslationDictionary } from "./vi";

export const enMessages: {
  common: Record<keyof TranslationDictionary["common"], string>;
  errors: Record<keyof TranslationDictionary["errors"], string>;
} = {
  common: {
    appName: "DAVS",
    home: "Home",
    explore: "Explore",
    projects: "Projects",
    datasources: "Data Sources",
    labels: "Data Labels",
    users: "Users",
    groups: "Groups",
    settings: "Settings",
    save: "Save",
    cancel: "Cancel",
    confirm: "Confirm",
    close: "Close",
    delete: "Delete",
    loading: "Loading...",
    emptyTitle: "No data yet",
    emptyDescription: "Create your first item to get started.",
    prevPage: "Previous page",
    nextPage: "Next page",
    currentRole: "Current role",
    collapseSidebar: "Collapse sidebar",
  },
  errors: {
    forbidden: "You do not have permission to perform this action.",
    sessionExpired: "Your session has expired. Please sign in again.",
    unknown: "An unexpected error occurred. Please try again.",
  },
};
