package com.squeezecontrol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.squeezecontrol.io.SqueezeCommand;
import com.squeezecontrol.io.SqueezeTaggedRequestBuilder;
import com.squeezecontrol.model.Artist;
import com.squeezecontrol.model.XmlBrowser;
import com.squeezecontrol.view.BrowseableAdapter;

/**
 * The roots of apps and radios hav a special query and response format, so it
 * cannot be the same as {@link XmlBrowserActivity}.
 * 
 * @author liodden
 * 
 */
public class AppsAndRadioRootsBrowserActivity extends
		AbstractMusicBrowserActivity<XmlBrowser> {

	public static final String EXTRA_BROWSER_TYPE = "browser_type";

	public static final String RADIOS = "radios";
	public static final String APPS = "apps";

	private String mBrowserType;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mBrowserType = getIntent().getStringExtra(EXTRA_BROWSER_TYPE);

		setContentView(R.layout.default_browser_list);
		super.init();
	}

	@Override
	protected BrowseableAdapter<XmlBrowser> createListAdapter() {
		return new BrowseableAdapter<XmlBrowser>(this,
				android.R.layout.simple_list_item_1);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		XmlBrowser item = (XmlBrowser) l.getItemAtPosition(position);
		if (item == null)
			return;
		Intent i = new Intent(this, XmlBrowserActivity.class);

		i.putExtra(XmlBrowserActivity.EXTRA_BROWSER_COMMAND_COMMAND, item.cmd);
		i.putExtra(XmlBrowserActivity.EXTRA_BROWSER_TITLE, item.name);
		if ("xmlbrowser_search".equals(item.type)) {
			i.putExtra(XmlBrowserActivity.EXTRA_SEARCH_MODE, true);
		}
		startActivity(i);
	}

	@Override
	protected void addToPlaylist(XmlBrowser selectedItem) {
	}

	@Override
	protected void play(XmlBrowser selectedItem, int index) {
	}

	@Override
	protected BrowseLoadResult<XmlBrowser> loadItems(int startIndex,
			int pageSize) throws IOException {
		ArrayList<XmlBrowser> radios = new ArrayList<XmlBrowser>(pageSize);
		int count = 0;
		try {
			SqueezeTaggedRequestBuilder command = new SqueezeTaggedRequestBuilder(
					mBrowserType + " " + startIndex + " " + pageSize);
			SqueezeCommand res = getSqueezeService().getBroker().sendRequest(
					command.toString());

			// Count comes first here
			for (String c : res.getParameters()) {
				if (c.startsWith("count%3A")) {
					count = Integer.parseInt(c.substring("count%3A".length()));
					break;
				}
			}
			List<Map<String, String>> maps = res.splitToMap("icon");
			for (Map<String, String> m : maps) {
				XmlBrowser radio = new XmlBrowser();
				radio.icon = m.get("icon");
				radio.name = m.get("name");
				radio.cmd = m.get("cmd");
				radio.type = m.get("type");
				radios.add(radio);
			}
		} catch (IOException e) {
		}
		return new BrowseLoadResult<XmlBrowser>(count, startIndex, radios);
	}
}
