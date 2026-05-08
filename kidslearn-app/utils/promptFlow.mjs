export function getAutoCheckinDecision({
  gradePopupVisible,
  gradeSetupRequired = false,
  alreadyAutoOpened
}) {
  if (alreadyAutoOpened) {
    return { shouldOpen: false, shouldRememberPending: false }
  }

  if (gradePopupVisible || gradeSetupRequired) {
    return { shouldOpen: false, shouldRememberPending: true }
  }

  return { shouldOpen: true, shouldRememberPending: false }
}

