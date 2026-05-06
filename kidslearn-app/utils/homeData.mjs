export function createHomeDataRequests(api, gradeLevelId) {
  return [
    api.getDailyTasks(),
    api.getSubjects(gradeLevelId),
    api.getPetStatus(),
    api.getRanking('weekly').catch(() => null),
    api.getMyProgress().catch(() => null)
  ]
}
