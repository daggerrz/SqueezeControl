package com.squeezecontrol;

import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squeezecontrol.io.SqueezePlayer;

public class BrowsePlayersActivity extends ListActivity {
	
	private ArrayList<SqueezePlayer> mPlayers;
	private ArrayAdapter<SqueezePlayer> mPlayerAdaper;
	private LayoutInflater mInflater;
	
	private String mCurrentPlayerId = null;

	protected SqueezeService mService;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
			mCurrentPlayerId = Settings.getPlayerId(this);
		
		mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mPlayers = new ArrayList<SqueezePlayer>();
		mPlayerAdaper = new ArrayAdapter<SqueezePlayer>(this, 0, mPlayers) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
		        View view;

		        if (convertView == null) {
		            view = mInflater.inflate(R.layout.player_list_item, parent, false);
		        } else {
		            view = convertView;
		        }
		        
		        SqueezePlayer player = mPlayers.get(position);

		        TextView nameView = (TextView) view.findViewById(R.id.player_name);
		        TextView modelView = (TextView) view.findViewById(R.id.player_model);
		        TextView ipView = (TextView) view.findViewById(R.id.player_ip);
		        
		        nameView.setText(player.name);
		        modelView.setText(player.model);
		        ipView.setText(player.ipAddress);
		        return view;
			}
		};
		setListAdapter(mPlayerAdaper);
		
		ServiceUtils.bindToService(this, new SqueezeServiceConnection() {
			@Override
			public void onServiceConnected(SqueezeService service) {
				mService = service;
				mPlayers.clear();
				try {
					mPlayers.addAll(service.getBroker().getPlayers());
					if (mCurrentPlayerId != null) {
						int i = 0;
						for (SqueezePlayer p : mPlayers) {
							if (p.getId().equals(mCurrentPlayerId)) {
								getListView().setSelection(i);
								break;
							}
							i++;
						}
					}
					mPlayerAdaper.notifyDataSetChanged();
					
				} catch (IOException e) {
					Toast.makeText(BrowsePlayersActivity.this, "Unable to get player list: " + e.getMessage(), Toast.LENGTH_LONG);
				}
			}
		});
	}
	
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		SqueezePlayer player = mPlayerAdaper.getItem(position);
		mService.setPlayer(player);
		Intent data = new Intent();
		data.putExtra(PlayerActivity.EXTRA_PLAYER_ID, player.getId());
		setResult(Activity.RESULT_OK, data);
		Settings.setPlayerId(this, player.getId());
		
		finish();
	}
	
	@Override
	protected void onDestroy() {
		ServiceUtils.unbindFromService(this);
		super.onDestroy();
	}
}
