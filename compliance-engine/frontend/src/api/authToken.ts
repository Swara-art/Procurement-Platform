let accessToken: string | null = null;
export const getAccessToken = () => accessToken;
export const setAccessToken = (value: string | null) => { accessToken = value; };
