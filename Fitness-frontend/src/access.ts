/**
 * @see https://umijs.org/docs/max/access#access
 * */
export default function access(initialState: { currentUser?: API.CurrentUser }) {
  const currentUser = initialState?.currentUser;

  return {
    canAdmin: currentUser?.userRole === 1,
  };
}
