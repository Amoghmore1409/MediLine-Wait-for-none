const fs = require('fs');
const f = 'app/src/main/java/com/example/mediline/QueueManagementActivity.java';
let content = fs.readFileSync(f, 'utf8');
const oldStr = content.substring(content.indexOf('StorageReference storageRef = Firebase'), content.indexOf('} catch (Exception ex)'));
const newStr = com.cloudinary.android.MediaManager.get().upload(bytes)
                    .option("folder", "Images/Prescriptions")
                    .callback(new com.cloudinary.android.callback.UploadCallback() {
                        @Override
                        public void onStart(String requestId) { }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) { }

                        @Override
                        public void onSuccess(String requestId, java.util.Map resultData) {
                            String downloadUrl = (String) resultData.get("secure_url");
                            progress.dismiss();
                            appointmentRepo.updateAppointmentField(targetId, "prescriptionUrl", downloadUrl, updateTask -> {
                                if (updateTask.isSuccessful()) {
                                    android.widget.Toast.makeText(QueueManagementActivity.this, "Prescription uploaded successfully!", android.widget.Toast.LENGTH_SHORT).show();
                                } else {
                                    android.widget.Toast.makeText(QueueManagementActivity.this, "Failed to update Database!", android.widget.Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onError(String requestId, com.cloudinary.android.callback.ErrorInfo error) {
                            progress.dismiss();
                            android.widget.Toast.makeText(QueueManagementActivity.this, "Upload failed: " + error.getDescription(), android.widget.Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onReschedule(String requestId, com.cloudinary.android.callback.ErrorInfo error) { }
                    }).dispatch();
        ;
fs.writeFileSync(f, content.replace(oldStr, newStr));
