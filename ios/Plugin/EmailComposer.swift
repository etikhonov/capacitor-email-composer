import Foundation
import Capacitor
import MessageUI

extension URL {
    var isDirectory: Bool {
       (try? resourceValues(forKeys: [.isDirectoryKey]))?.isDirectory == true
    }
}

enum AttachmentError: Error {
    case isDirectory
    case cantRaadFile
}

@objc public class EmailComposer: NSObject {

    func canSendMail() -> Bool {
        return MFMailComposeViewController.canSendMail();
    }
    
    func addAttachmentToMailDraft(mailDraft: MFMailComposeViewController, path: String) throws {
        if let url = URL(string: path) {
            if(url.isDirectory){
                throw AttachmentError.isDirectory
            }
            do {
                let data = try Data(contentsOf: url)
                mailDraft.addAttachmentData(data, mimeType: "application/octet-stream", fileName:  url.lastPathComponent);
            } catch {
                print("Raad attachment file error: \(error)")
                throw AttachmentError.cantRaadFile
            }
        }
    }

    func getMailComposerFromCall(_ call: CAPPluginCall, delegateTo: MFMailComposeViewControllerDelegate) -> MFMailComposeViewController {
        let draft = MFMailComposeViewController();

        // Subject
        draft.setSubject(call.getString("subject", ""));

        // Body
        draft.setMessageBody(call.getString("body", ""), isHTML: call.getBool("isHtml", false));

        // TO
        draft.setToRecipients(call.getArray("to", String.self));

        // CC
        draft.setCcRecipients(call.getArray("cc", String.self));

        // BCC
        draft.setBccRecipients(call.getArray("bcc", String.self));
        
        // Attachments
        let attachments = call.options["attachments"]
        do {
            if attachments! is String {
                try addAttachmentToMailDraft(mailDraft: draft, path: attachments! as! String)
            }
            if attachments! is Array<String> {
                for attachment in attachments as! [String] {
                    try addAttachmentToMailDraft(mailDraft: draft, path: attachment)
                }
            }
        } catch AttachmentError.cantRaadFile {
            call.reject("Fail to load attachment " + (attachments! as! String))
        } catch AttachmentError.isDirectory {
            call.reject("Attachment path is a Directory but should be a file")
        } catch {
            call.reject("Unexpected error \(error.localizedDescription)")
        }
       

        draft.mailComposeDelegate = delegateTo;
        return draft;
    }
}
