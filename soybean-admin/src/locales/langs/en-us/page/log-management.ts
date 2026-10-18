const local: App.I18n.Schema['page']['logManagement'] = {
  common: {
    time: 'Time',
    elapsedMs: 'Elapsed (ms)',
    errorMsg: 'Error Message'
  },
  logFile: {
    title: 'Log File',
    fileName: 'File Name',
    size: 'Size',
    lastModifiedAt: 'Modified At',
    compressed: 'Compressed',
    preview: 'Preview',
    previewTitle: 'Preview — {name}',
    lines: 'Lines',
    refreshPreview: 'Refresh Preview'
  },
  operationLog: {
    title: 'Operation Log',
    userName: 'User Name',
    user: 'User',
    operation: 'Operation',
    requestUri: 'Request URI',
    requestMethod: 'Request Method',
    method: 'Method',
    isSuccess: 'Success',
    classMethod: 'Class.Method',
    className: 'Class Name',
    methodName: 'Method Name',
    errorType: 'Error Type',
    requestPayload: 'Request Payload',
    responsePayload: 'Response Payload',
    detailTitle: 'Operation Log Detail - {operation}'
  }
};

export default local;
