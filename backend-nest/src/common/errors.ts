/** One error type for everything the API reports; the filter turns it into the Spring-style ErrorResponse. */
export class ApiError extends Error {
  /** Answer with an empty body (Spring's 401 entry point does that). */
  empty = false;

  constructor(
    public readonly status: number,
    message: string,
    public readonly details: unknown = null,
  ) {
    super(message);
  }
}

export const notFound = (resource: string, id: number | string) =>
  new ApiError(404, `${resource} not found with id: ${id}`);
export const notFoundMessage = (message: string) => new ApiError(404, message);
export const validation = (message: string) => new ApiError(400, message);
export const invalidCredentials = () => new ApiError(401, 'Invalid username or password');
export const forbidden = () => new ApiError(403, 'Access denied');
export const versionConflict = () =>
  new ApiError(409, 'This record was changed by someone else. Reload it and try again.');
export const malformedBody = () => new ApiError(400, 'Malformed request body');
export const badParameter = (name: string) => new ApiError(400, `Invalid value for parameter '${name}'`);
export const validationFailed = (details: Record<string, string>) =>
  new ApiError(400, 'Validation failed', details);

export const unauthenticated = () => {
  const error = new ApiError(401, 'Unauthorized');
  error.empty = true;
  return error;
};
